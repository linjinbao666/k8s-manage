package monitor.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.AtomicDouble;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import monitor.enumlation.CodeEnum;
import monitor.service.ResourceService;
import monitor.vo.K8sResourceVo;
import monitor.vo.ResultVo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    KubernetesClient kubernetesClient;
    @Autowired
    RestTemplate restTemplate;
    @Value("${prometheus.queryRange.url}")
    String queryRangeUrl;

    @Override
    public List<K8sResourceVo> allCpuAndMemory() {
        AtomicLong cpuAll = new AtomicLong(0l);
        AtomicLong memoryAll = new AtomicLong(0l);
        AtomicDouble cpuUsed = new AtomicDouble(0.0);
        AtomicDouble memoryUsed = new AtomicDouble(0.0);
        NodeList nodeList = kubernetesClient.nodes().list();
        nodeList.getItems().forEach(item ->{
            Map<String, Quantity> allocatable = item.getStatus().getAllocatable();
            String cpu = allocatable.get("cpu").getAmount();
            String memory = allocatable.get("memory").getAmount();
            cpuAll.updateAndGet(v -> v + Long.valueOf(cpu));
            memoryAll.updateAndGet(v -> v + Long.valueOf(memory)/1000000);
        });
        PodMetricsList podMetricsList = kubernetesClient.top().pods().metrics();
        podMetricsList.getItems().forEach(item -> {
            item.getContainers().forEach(container -> {
                String cpu = container.getUsage().get("cpu").getAmount();
                String memory = container.getUsage().get("memory").getAmount();
                cpuUsed.addAndGet(Long.valueOf(cpu).doubleValue()/1000000000);
                memoryUsed.addAndGet(Long.valueOf(memory).doubleValue()/1000000);
            });
        });
        K8sResourceVo cpuVo = new K8sResourceVo();
        DecimalFormat df = new DecimalFormat("0.00");
        cpuVo.setName("cpu");
        cpuVo.setAll(Double.parseDouble(df.format(cpuAll.doubleValue())));
        cpuVo.setUsed(Double.parseDouble(df.format(cpuUsed.doubleValue())));
        cpuVo.setAvaliable(Double.parseDouble(df.format(cpuAll.get() - cpuUsed.get())));
        cpuVo.setPercentage(cpuVo.getAvaliable()/cpuVo.getAll());
        K8sResourceVo memoryVo = new K8sResourceVo();
        memoryVo.setName("memory");
        memoryVo.setUsed(memoryUsed.doubleValue());
        memoryVo.setAll(memoryAll.doubleValue());
        memoryVo.setAvaliable(memoryAll.get() - memoryUsed.get());
        memoryVo.setPercentage(memoryVo.getAvaliable()/ memoryVo.getAll());
        List list = new ArrayList();
        list.add(cpuVo);
        list.add(memoryVo);
        return list;
    }

    @Override
    public ResultVo getCpuRange(String appName, String containerName, String chronoUnit) {
//        String query = "container_cpu_usage_seconds_total{container_name='containerName',namespace='fline'}";
        String query = "rate(container_cpu_usage_seconds_total{pod_name=~\"containerName.*\",namespace!=\"\",image!=\"\"}[1m])*1000000";
        String start = null;
        String end = null;
        String step = null;

        switch (chronoUnit){
            case "DAY":
                end = String.valueOf(Instant.now().getEpochSecond());
                start = String.valueOf(LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.ofHours(8)).getEpochSecond());
                step = "1h";
                break;
            case "WEEK":
                end = String.valueOf(Instant.now().getEpochSecond());
                start = String.valueOf(LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.ofHours(8)).getEpochSecond());
                step = "1d";
                break;
            case "MONTH":
                end = String.valueOf(Instant.now().getEpochSecond());
                start = String.valueOf(LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.ofHours(8)).getEpochSecond());
                step = "1d";
                break;
            default:
                break;
        }

        String finalStart = start;
        String finalEnd = end;
        String finalStep = step;
        query = query.replace("containerName", containerName);
        String finalQuery = query;
        log.info("query = " + query);
        MultiValueMap<String, String> params= new LinkedMultiValueMap<String, String>(){{
            add("query", finalQuery);
            add("start", finalStart);
            add("end", finalEnd);
            add("step", finalStep);
        }};
        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<MultiValueMap<String, String>>(params, null);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(queryRangeUrl);
        URI uri = builder.queryParams(params).build().encode().toUri();

        ResponseEntity<String> exchange = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
        int statusCode = exchange.getStatusCodeValue();
        if (statusCode != 200) return ResultVo.renderErr().withRemark("查询失败");
        String body = exchange.getBody();
        return ResultVo.renderOk(body).withRemark("查询cpu使用率成功cpu使用率！");
    }

    private List<Map> getContainerResult(JSONObject res){
        List<Map> mapList = new ArrayList<>();

        JSONObject data = (JSONObject) res.get("data");
        JSONArray result = (JSONArray) data.get("result");
        for (int i = 0; i< result.size(); i++){
            Map<String, Object> vo = new HashMap();
            JSONObject jsonStr =(JSONObject) result.get(i);
            JSONObject metric = (JSONObject) jsonStr.get("metric");
            JSONArray values = (JSONArray) jsonStr.get("values");
            String podName = (String) metric.get("pod_name");
            List valueList = new ArrayList();
            for (int j =0; j<values.size(); j++){
                JSONArray tmp = (JSONArray) values.get(j);
                Map m = new HashMap();
                m.put("key", tmp.get(0));
                m.put("val", String.format("%.3f",  Double.parseDouble(String.valueOf(tmp.get(1)))));
                valueList.add(m);
            }
            vo.put("name", podName);
            vo.put("values", valueList);
            mapList.add(vo);
        }
        return mapList;
    }

    @Override
    public ResultVo getMemoryRange(String appName, String containerName, String chronoUnit) {
//        String query = "container_memory_usage_bytes{container_name='containerName',namespace='fline'}";
        String query = "container_memory_working_set_bytes{pod_name=~\"containerName.*\",namespace!=\"\",image!=\"\"}/1000000";
        String start = null;
        String end = null;
        String step = null;

        switch (chronoUnit){
            case "DAY":
                end = String.valueOf(Instant.now().getEpochSecond());
                start = String.valueOf(LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.ofHours(8)).getEpochSecond());
                step = "1h";
                break;
            case "WEEK":
                end = String.valueOf(Instant.now().getEpochSecond());
                start = String.valueOf(LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.ofHours(8)).getEpochSecond());
                step = "1d";
                break;
            case "MONTH":
                end = String.valueOf(Instant.now().getEpochSecond());
                start = String.valueOf(LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.ofHours(8)).getEpochSecond());
                step = "1d";
                break;
            default:
                break;
        }

        String finalStart = start;
        String finalEnd = end;
        String finalStep = step;
        query = query.replace("containerName", containerName);
        String finalQuery = query;
        log.info("query = " + query);
        MultiValueMap<String, String> params= new LinkedMultiValueMap<String, String>(){{
            add("query", finalQuery);
            add("start", finalStart);
            add("end", finalEnd);
            add("step", finalStep);
        }};
        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<MultiValueMap<String, String>>(params, null);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(queryRangeUrl);
        URI uri = builder.queryParams(params).build().encode().toUri();

        ResponseEntity<String> exchange = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
        int statusCode = exchange.getStatusCodeValue();
        if (statusCode != 200) return ResultVo.renderErr().withRemark("查询失败");

        return ResultVo.renderOk(exchange.getBody()).withRemark("查询内存使用率成功！");
    }

    @Override
    public ResultVo getIORange(String appName, String containerName, String chronoUnit) {
//        String query = "container_fs_io_time_seconds_total{container_name='containerName'}";
        String query = "sum(rate(container_network_receive_bytes_total{id!=\"/\",pod_name=~\"containerName.*\"}[1m]))by(pod_name)/1024";
        String start = null;
        String end = null;
        String step = null;

        switch (chronoUnit){
            case "DAY":
                end = String.valueOf(Instant.now().getEpochSecond());
                start = String.valueOf(LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.ofHours(8)).getEpochSecond());
                step = "1h";
                break;
            case "WEEK":
                end = String.valueOf(Instant.now().getEpochSecond());
                start = String.valueOf(LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.ofHours(8)).getEpochSecond());
                step = "1d";
                break;
            case "MONTH":
                end = String.valueOf(Instant.now().getEpochSecond());
                start = String.valueOf(LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.ofHours(8)).getEpochSecond());
                step = "1d";
                break;
            default:
                break;
        }

        String finalStart = start;
        String finalEnd = end;
        String finalStep = step;
        query = query.replace("containerName", containerName);
        String finalQuery = query;
        log.info("query = " + query);
        MultiValueMap<String, String> params= new LinkedMultiValueMap<String, String>(){{
            add("query", finalQuery);
            add("start", finalStart);
            add("end", finalEnd);
            add("step", finalStep);
        }};
        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<MultiValueMap<String, String>>(params, null);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(queryRangeUrl);
        URI uri = builder.queryParams(params).build().encode().toUri();

        ResponseEntity<String> exchange = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
        int statusCode = exchange.getStatusCodeValue();
        if (statusCode != 200) return ResultVo.renderErr().withRemark("查询失败");
        return ResultVo.renderOk(exchange.getBody()).withRemark("查询IO使用率成功！");
    }

    @Override
    public ResultVo apps() {
        List<Deployment> items = kubernetesClient.apps().deployments().inAnyNamespace().list().getItems();
        List<Map> apps = new ArrayList<>();
        items.forEach(item ->{
            String namespace = item.getMetadata().getNamespace();
            String name = item.getMetadata().getName();
            apps.add(new HashMap(){{put(namespace, name);}});
        });
        return ResultVo.renderOk(apps).withRemark("查询所有应用成功！");
    }

    @Override
    public ResultVo apps2() {
        List<Deployment> items = kubernetesClient.apps().deployments().inAnyNamespace().list().getItems();
        List<Map> apps = new ArrayList<>();
        items.forEach(item ->{
            String namespace = item.getMetadata().getNamespace();
            String name = item.getMetadata().getName();
            apps.add(new HashMap(){{
                put("namespace", namespace);
                put("appName",name);}});
        });
        return ResultVo.renderOk(apps).withRemark("查询所有应用成功！");
    }

    @Override
    public ResultVo containers(String namespace, String appName) {
        Deployment deployment = kubernetesClient.apps().deployments().inNamespace(namespace).withName(appName).get();
        if (null== deployment) return ResultVo.renderErr(CodeEnum.ERR).withRemark("查询失败，可能目标应用不存在");
        List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
        List<Map<String,String>> containers2 = new ArrayList<>();
        containers.forEach(container -> {
            containers2.add(new HashMap<String, String>(){{
                put("namespace",namespace);
                put("appName",appName);
                put("containerName",container.getName());
            }});
        });

        return ResultVo.renderOk(containers2).withRemark("查询容器成功！");
    }


}
