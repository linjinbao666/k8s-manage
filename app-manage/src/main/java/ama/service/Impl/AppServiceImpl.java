package ama.service.Impl;

import ama.dao.AppDao;
import ama.entity.App;
import ama.exception.BizException;
import ama.service.AppService;
import ama.enumlation.CodeEnum;
import ama.service.IngressService;
import ama.util.MyUtil;
import ama.vo.AppPodVo;
import ama.vo.K8sResourceVo;
import ama.vo.ResultVo;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.ExposedPort;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.*;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta2.DoneableHorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta2.HorizontalPodAutoscalerBuilder;
import io.fabric8.kubernetes.api.model.autoscaling.v2beta2.MetricSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.directory.api.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AppServiceImpl implements AppService{
    @Autowired
    AppDao appDao;
    @Autowired
    KubernetesClient kubernetesClient;
    @Autowired
    IngressService ingressService;
    @Override
    public void syncApp(String namespace) {
        List<App> apps = new ArrayList<>();
        List<Deployment> items = kubernetesClient.apps().deployments().inNamespace(namespace).list().getItems();
        for(int i=0; i< items.size(); i++){
            Deployment deployment = items.get(i);
            try {
                syncOne(namespace, deployment.getMetadata().getName());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        appDao.saveAll(apps);
    }

    @Override
    public void syncOne(String namespace, String appName) {
        Deployment item = kubernetesClient.apps().deployments().inNamespace(namespace).withName(appName).get();
        if(null == item) {
            App one = appDao.findOne(namespace, appName);
            if (one==null) return;
            one.setStatus("stop");
            appDao.save(one);
            return;
        }
        App app = new App();
        String name = item.getMetadata().getName();
        app.setAppName(name);
        app.setNamespace(namespace);
        List<Pod> podList = kubernetesClient.pods().inNamespace(namespace).withLabel("name", name).list().getItems();
        if (podList.isEmpty()){ podList = kubernetesClient.pods().inNamespace(namespace).withLabel("app", name).list().getItems(); }
        if (null!= podList && podList.size()>0){
            String hostIP = podList.get(0).getStatus().getHostIP();
            String status = podList.get(0).getStatus().getPhase();
            app.setHostIP(hostIP);
            app.setStatus(status);
        }
        io.fabric8.kubernetes.api.model.Service service = kubernetesClient.services().inNamespace(namespace).withName(name).get();
        String clusterIP = service.getSpec().getClusterIP();
        Integer nodePort = 0;
        if(service.getSpec().getType().equals("NodePort")){ nodePort = service.getSpec().getPorts().get(0).getNodePort(); }
        IntOrString targetPort = service.getSpec().getPorts().get(0).getTargetPort();
        String externalTrafficPolicy = service.getSpec().getExternalTrafficPolicy();
        app.setClusterIP(clusterIP);
        app.setInnerAddress(clusterIP+":"+service.getSpec().getPorts().get(0).getPort());
        app.setExternalTrafficPolicy(externalTrafficPolicy);
        app.setTargetPort(targetPort.getStrVal());
        app.setNodePort(nodePort);
        String type = item.getSpec().getStrategy().getType();
        Integer replicas = item.getSpec().getReplicas();
        Map<String, String> nodeSelector = item.getSpec().getTemplate().getSpec().getNodeSelector();
        if (null != nodeSelector){ nodeSelector.forEach((key, value)-> app.setNodeSelector(key+"="+value)); }
        app.setUpdatePolicy(type);
        app.setReplicas(replicas);
        item.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
            String image = container.getImage();
            String imagePullPolicy = container.getImagePullPolicy();
            String cpuAmount = "", cpuFormat = "", memoryAmount ="", memoryFormat= "";
            if(null!=container.getResources().getLimits()){
                cpuAmount = container.getResources().getLimits().get("cpu").getAmount().trim();
                cpuFormat = container.getResources().getLimits().get("cpu").getFormat().trim();
                memoryAmount = container.getResources().getLimits().get("memory").getAmount().trim();
                memoryFormat = container.getResources().getLimits().get("memory").getFormat().trim();
            }
            if (Strings.isEmpty(cpuFormat) && Strings.isNotEmpty(cpuAmount)){
                cpuAmount = String.valueOf(Integer.valueOf(cpuAmount)*1000);
                cpuFormat = "m";
            }
            if (memoryFormat.equalsIgnoreCase("Gi") && Strings.isNotEmpty(memoryAmount)){
                memoryAmount = String.valueOf(Integer.valueOf(memoryAmount)*1000);
                memoryFormat = "Mi";
            }
            Integer containerPort = container.getPorts().get(0).getContainerPort();
            String pvcnames = "", mounts = "";
            List<VolumeMount> volumeMounts = container.getVolumeMounts();
            if (!volumeMounts.isEmpty()){
                for (VolumeMount volumeMount : volumeMounts){
                    pvcnames = pvcnames + ","+ volumeMount.getName();
                    mounts = mounts + "," + volumeMount.getMountPath();
                }
                app.setPvcNames(pvcnames.substring(1));
                app.setMountPaths(mounts.substring(1));
            }
            StringBuilder envStr = new StringBuilder();
            List<EnvVar> envs = container.getEnv();
            envs.forEach(env ->{ envStr.append(";" + MyUtil.JdkBase64(env.getName()) + "," + MyUtil.JdkBase64(env.getValue())); });
            if(Strings.isNotEmpty(envStr.toString())){ app.setEnvs(envStr.substring(1)); }
            Probe livenessProbe = container.getLivenessProbe();
            if(null != livenessProbe){
                HTTPGetAction httpGet = livenessProbe.getHttpGet();
                if (null != httpGet){ app.setPointer(httpGet.getPath()); }
            }
            app.setImageName(image);
            app.setImagePolicy(imagePullPolicy);
            app.setCpuAmount(cpuAmount);
            app.setCpuFormat(cpuFormat);
            app.setMemoryAmount(memoryAmount);
            app.setMemoryFormat(memoryFormat);
            app.setContainerPort(containerPort);
        });
        app.setUrl(app.getHostIP()+":"+nodePort);
        App byName = appDao.findByAppName(app.getAppName());
        if (byName!=null) {
            app.setId(byName.getId());
            app.setCreateDate(byName.getCreateDate());
            app.setCreator(byName.getCreator());
            app.setDominName(byName.getDominName());
            app.setPvcNames(byName.getPvcNames());
            app.setMountPaths(byName.getMountPaths());
            app.setCnName(byName.getCnName());
            app.setServiceType(byName.getServiceType());
        }
        app.setServiceType("ClusterIP");
        long httpPort = ingressService.ingressHttpPort();
        if (null != ingressService.findOneIng(app)){
            app.setOutAddress(app.getHostIP() + ":" + httpPort + "/" + app.getAppName());
            app.setServiceType("INGRESS");
        }
        appDao.save(app);
    }

    @Override
    public long count(Map<String, Object> params) {
        Specification<App> specification = new Specification<App>() {
            @Override
            public Predicate toPredicate(Root<App> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaQuery.where(list.toArray(new Predicate[list.size()])).getRestriction();
            }
        };
        return appDao.count(specification);
    }

    @Override
    public List<App> findAll() {
        return appDao.findAll();
    }

    @Override
    public List<App> findAll(Map<String, Object> params, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Specification<App> specification = new Specification<App>() {
            @Override
            public Predicate toPredicate(Root<App> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.like(root.get(key).as(String.class), "%"+value+"%")); });
                return criteriaBuilder.and(list.toArray(new Predicate[0]));
            }
        };
        Page<App> pageObject = appDao.findAll(specification, pageable);
        List<App> apps = new ArrayList<>();
        pageObject.getContent().forEach(tmp -> { apps.add(tmp); });
        return apps;
    }

    @Override
    public App findOne(String namespace, String appName) {
        syncOne(namespace, appName);
        return appDao.findOne(namespace, appName);
    }

    @Autowired
    DockerClient dockerClient;

    @Override
    public ResultVo addOne(App app) {
        App one = appDao.findOne(app.getNamespace(), app.getAppName());
        if (null != one) { return ResultVo.renderErr(CodeEnum.ERR).withRemark("应用名称已经存在"); }
        App oneWithNodePort = appDao.findOneWithNodePort(app.getNodePort());
        if (null != app.getNodePort() && null!=oneWithNodePort) { return ResultVo.renderErr(CodeEnum.ERR).withRemark("端口已经占用"); }
        Deployment depRemote = kubernetesClient.apps().deployments().inNamespace(app.getNamespace()).withName(app.getAppName()).get();
        io.fabric8.kubernetes.api.model.Service serRemote = kubernetesClient.services().inNamespace(app.getNamespace()).withName(app.getAppName()).get();
        if (null != depRemote || null != serRemote) { return ResultVo.renderErr().withRemark("部署失败，远程服务器存在该名称，您可以手动同步下"); }
        if (Strings.isNotEmpty(app.getPvcNames()) && Strings.isNotEmpty(app.getMountPaths())){
            int len1 = app.getPvcNames().split(",").length;
            int len2 = app.getMountPaths().split(",").length;
            if (len1 != len2) { return ResultVo.renderErr(CodeEnum.ERR).withRemark("禁止部署,由于存储卷和挂载目录个数无法对应"); }
        }
        ExposedPort[] exposedPorts = null;
        try{
            exposedPorts = dockerClient.inspectImageCmd(app.getImageName()).exec().getContainerConfig().getExposedPorts();
        } catch (BizException e){
            e.printStackTrace();
            return ResultVo.renderErr(CodeEnum.EXPOSED_PORT_ERR);
        }
        if (exposedPorts!=null && exposedPorts.length>0){ app.setContainerPort(exposedPorts[0].getPort()); }
        Deployment deployment = null;
        io.fabric8.kubernetes.api.model.Service service = null;
        try {
            deployment = createDeployment(app);
            service = createService(app);
        } catch (BizException e) {
            e.printStackTrace();
            return ResultVo.renderErr();
        }
        try {
            deployment = kubernetesClient.apps().deployments().create(deployment);
            service = kubernetesClient.services().create(service);
            app.setStatus("pending");
        }catch (Exception e){
            e.printStackTrace();
            log.info("发布出错，即将回滚");
            kubernetesClient.apps().deployments().delete(deployment);
            kubernetesClient.services().delete(service);
            ingressService.delOneIng(app);
            return ResultVo.renderErr(CodeEnum.PUBLISH_ERR);
        }
        appDao.save(app);
        syncOne(app.getNamespace(), app.getAppName());
        return ResultVo.renderOk(app).withRemark("部署应用成功！，部分数据可能需要延时响应");
    }

    @Override
    public ResultVo deleteOne(String namespace, String appName) {
        App one = appDao.findOne(namespace, appName);
        if (null == one) return ResultVo.renderErr().withRemark("数据库不存在该应用");
        Boolean delete = kubernetesClient.apps().deployments().inNamespace(namespace).withName(appName).delete();
        try {
            if (one.getServiceType().equalsIgnoreCase("INGRESS")){ ingressService.delOneIng(one); }
            kubernetesClient.services().inNamespace(namespace).withName(appName).delete();
            appDao.delete(namespace, appName);
        }catch (Exception e){
            appDao.save(one);
            ingressService.addOneIng(one);
            return ResultVo.renderErr().withRemark("删除出错，操作已经回滚");
        }
        return ResultVo.renderOk().withRemark("删除操作已经提交，服务器可能存在延迟！");
    }

    @Override
    public ResultVo pauseOne(String namespace, String appName) {
        App one = appDao.findOne(namespace, appName);
        if (null == one) return ResultVo.renderErr().withRemark("数据库不存在该应用");
        Boolean delete = kubernetesClient.apps().deployments().inNamespace(namespace).withName(appName).delete();
        try {
            kubernetesClient.services().inNamespace(namespace).withName(appName).delete();
            if (one.getServiceType().equalsIgnoreCase("INGRESS")){ ingressService.delOneIng(one); }
            one.setStatus("stop");
        }catch (Exception e){
            ingressService.addOneIng(one);
            return ResultVo.renderErr().withRemark("删除出错，操作已经回滚");
        }
        one.setStatus("stop");
        appDao.save(one);
        return ResultVo.renderOk().withRemark("停止操作提交，服务器可能存在延迟");
    }

    @Override
    public ResultVo updateOne(App app) throws Exception {
        App one = appDao.findOne(app.getNamespace(), app.getAppName());
        if (null == one) return ResultVo.renderErr().withRemark("数据库不存在该应用");
        app.setId(one.getId());
        if (null != app.getNodePort() && !app.getNodePort().equals(one.getNodePort())){
            boolean contains = usedNodePorts().contains(app.getNodePort());
            if (contains) return ResultVo.renderErr().withRemark("修改端口失败，目标端口已经占用");
        }
        if (Strings.isNotEmpty(app.getPvcNames()) && Strings.isNotEmpty(app.getMountPaths())){
            int len1 = app.getPvcNames().split(",").length, len2 = app.getMountPaths().split(",").length;
            if (len1 != len2) { return ResultVo.renderErr(CodeEnum.ERR).withRemark("禁止部署,由于存储卷和挂载目录个数无法对应"); }
        }
        Deployment depRemote = kubernetesClient.apps().deployments().inNamespace(app.getNamespace()).withName(app.getAppName()).get();
        io.fabric8.kubernetes.api.model.Service serRemote = kubernetesClient.services().inNamespace(app.getNamespace()).withName(app.getAppName()).get();
        if (null != depRemote) { kubernetesClient.apps().deployments().inNamespace(app.getNamespace()).withName(app.getAppName()).delete(); }
        if (null != serRemote){ kubernetesClient.services().inNamespace(app.getNamespace()).withName(app.getAppName()).delete(); }
        ExposedPort[] exposedPorts = null;
        try{
            exposedPorts = dockerClient.inspectImageCmd(app.getImageName()).exec().getContainerConfig().getExposedPorts();
        } catch (Exception e){
            e.printStackTrace();
            return ResultVo.renderErr(CodeEnum.EXPOSED_PORT_ERR);
        }
        if (exposedPorts!=null && exposedPorts.length>0){ app.setContainerPort(exposedPorts[0].getPort()); }
        Deployment deployment = null;
        io.fabric8.kubernetes.api.model.Service service = null;
        try {
            deployment = createDeployment(app);
            service = createService(app);
        } catch (BizException e) {
            e.printStackTrace();
            return ResultVo.renderErr();
        }
        try {
            deployment = kubernetesClient.apps().deployments().create(deployment);
            service = kubernetesClient.services().create(service);
            app.setStatus("pending");
        }catch (Exception e){
            e.printStackTrace();
            log.info("更新出错，即将回滚");
            kubernetesClient.apps().deployments().delete(deployment);
            kubernetesClient.services().delete(service);
            kubernetesClient.apps().deployments().createOrReplace(depRemote);
            kubernetesClient.services().createOrReplace(serRemote);
            ingressService.delOneIng(app);
            return ResultVo.renderErr(CodeEnum.UPDATE_ERR);
        }
        appDao.save(app);
        syncOne(app.getNamespace(), app.getAppName());
        return ResultVo.renderOk(app).withRemark("更新应用成功！，部分数据可能需要延时响应");
    }

    @Override
    public List<Integer> usedNodePorts() {
        return appDao.findNodePorts();
    }

    @Override
    public ResultVo pods(String namespace, String appName) {
        App one = appDao.findOne(namespace, appName);
        if (null == one) return ResultVo.renderErr().withRemark("数据库不存在该应用");
        List<AppPodVo> vos = new ArrayList<>();
        List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).withLabel("name", appName).list().getItems();
        if(pods.isEmpty()) pods = kubernetesClient.pods().inNamespace(namespace).withLabel("app", appName).list().getItems();
        pods.forEach(pod ->{
            AppPodVo vo  = new AppPodVo();
            vo.setPodName(pod.getMetadata().getName());
            vo.setStartTime(pod.getStatus().getStartTime());
            vo.setCreatedTime(pod.getMetadata().getCreationTimestamp());
            vo.setIp(pod.getStatus().getHostIP());
            if (pod.getStatus().getContainerStatuses().size()>0)vo.setRestartCount(pod.getStatus().getContainerStatuses().get(0).getRestartCount());
            vo.setStatus(pod.getStatus().getPhase());
            vo.setPort(one.getNodePort());
            vos.add(vo);
        });
        return ResultVo.renderOk(vos).withRemark("查询应用副本成功！");
    }

    @Override
    public Map nodeSelectors() {
        Map<String, List<String>> tagMaps = new HashMap<String, List<String>>();
        List<Node> list = kubernetesClient.nodes().list().getItems();
        for(int i=0; i < list.size(); i++) {
            Node node = list.get(i);
            Map<String, String> labels = node.getMetadata().getLabels();
            labels.forEach((key, value) -> {
                String ip = null;
                List<NodeAddress> addresses = node.getStatus().getAddresses();
                for(NodeAddress address : addresses) {
                    if(address.getType().equals("InternalIP"))ip = address.getAddress();
                }
                List<String> tmpList = new ArrayList<String>();
                if(tagMaps.containsKey(key+"="+value)) {
                    tmpList = tagMaps.get(key+"="+value);
                    tmpList.add(ip);
                    tagMaps.put(key+"="+value, tmpList);
                }else {
                    tmpList.add(ip);
                    tagMaps.put(key+"="+value, tmpList);
                }

            });
        }
        return tagMaps;
    }

    @Override
    public InputStream podLog(String namespace, String podName, Integer lines) {
        String log = kubernetesClient.pods().inNamespace(namespace)
                .withName(podName).tailingLines(lines).getLog();
        return new ByteArrayInputStream(log.getBytes());
    }

    @Override
    public ResultVo resourceUsed(String namespace) {
        Namespace exist = kubernetesClient.namespaces().withName(namespace).get();
        if (null == exist) return ResultVo.renderErr().withRemark("名称空间不存在");

        Map<String, Quantity> all = kubernetesClient.resourceQuotas()
                .inNamespace(namespace)
                .withName(namespace)
                .get()
                .getStatus()
                .getHard();
        Map<String, Quantity> used = kubernetesClient.resourceQuotas().inNamespace(namespace)
                .withName(namespace)
                .get()
                .getStatus()
                .getUsed();
        Quantity cpuAll = all.get("cpu");
        Quantity memoryAll = all.get("memory");
        Quantity cpuUsed = used.get("cpu");
        Quantity memoryUsed = used.get("memory");
        assert cpuAll != null && memoryAll!=null && cpuUsed!=null && memoryUsed!=null;

        K8sResourceVo cpuVo = new K8sResourceVo();
        cpuVo.setName("cpu");
        String cpuAllFormat = cpuAll.getFormat();
        String cpuUsedFormat = cpuUsed.getFormat();
        if (Strings.isEmpty(cpuAllFormat)) {
            cpuVo.setAll(Long.parseLong(cpuAll.getAmount())*1000);
        }else {
            cpuVo.setAll(Long.parseLong(cpuAll.getAmount()));
        }
        if (Strings.isEmpty(cpuUsedFormat)){
            cpuVo.setUsed(Long.parseLong(cpuUsed.getAmount())*1000);
        }else {
            cpuVo.setUsed(Long.parseLong(cpuUsed.getAmount()));
        }

        K8sResourceVo memoryVo = new K8sResourceVo();
        memoryVo.setName("memory");
        String memoryAllFormat = memoryAll.getFormat();
        String memoryUsedFormat = memoryUsed.getFormat();
        if (memoryAllFormat.equals("Gi")) {
            memoryVo.setAll(Long.parseLong(memoryAll.getAmount())*1000);
        }else {
            memoryVo.setAll(Long.parseLong(memoryAll.getAmount()));
        }
        if (memoryUsedFormat.equals("Gi")){
            memoryVo.setUsed(Long.parseLong(memoryUsed.getAmount())*1000);
        }else {
            memoryVo.setUsed(Long.parseLong(memoryUsed.getAmount()));
        }
        return ResultVo.renderOk(new ArrayList<K8sResourceVo>(){{
            add(cpuVo);
            add(memoryVo);
        }}).withRemark("查询厂商资源使用成功！");
    }

    @Override
    public ResultVo hpa(String namespace, String appName,
                                  Integer min, Integer max,
                                  Integer cpuPercentage, Integer memoryPercentage) {
        HorizontalPodAutoscaler horizontalPodAutoscaler = new HorizontalPodAutoscalerBuilder()
                .withNewMetadata().withName(appName).withNamespace(namespace).endMetadata()
                .withNewSpec()
                .withNewScaleTargetRef()
                .withApiVersion("apps/v1")
                .withKind("Deployment")
                .withName(appName)
                .endScaleTargetRef()
                .withMinReplicas(min)
                .withMaxReplicas(max)
                .addToMetrics(new MetricSpecBuilder()
                        .withType("Resource")
                        .withNewResource()
                        .withName("cpu")
                        .withNewTarget()
                        .withType("Utilization")
                        .withAverageUtilization(cpuPercentage)
                        .endTarget()
                        .endResource()
                        .build())
                .withNewBehavior()
                .withNewScaleDown()
                .addNewPolicy()
                .withType("Pods")
                .withValue(4)
                .withPeriodSeconds(60)
                .endPolicy()
                .addNewPolicy()
                .withType("Percent")
                .withValue(cpuPercentage)
                .withPeriodSeconds(60)
                .endPolicy()
                .endScaleDown()
                .endBehavior()
                .endSpec()
                .build();
        kubernetesClient.autoscaling().v2beta2().horizontalPodAutoscalers().inNamespace(namespace).createOrReplace(horizontalPodAutoscaler);
        return ResultVo.renderOk().withRemark("扩容成功！");
    }

    @Override
    public ResultVo deleteHpa(String namespace, String appName){
        Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler> autoscalerResource = kubernetesClient.autoscaling().v2beta2().horizontalPodAutoscalers().inNamespace(namespace).withName(appName);
        if (null == autoscalerResource.get()){
            return ResultVo.renderErr().withRemark("目标hpa不存在");
        }
        Boolean delete = autoscalerResource.delete();
        if (delete) return ResultVo.renderOk().withRemark("删除操作提交，可能存在延时");
        return ResultVo.renderErr().withRemark("删除失败");
    }

    @Override
    public ResultVo deletePod(String namespace, String podName) {
        kubernetesClient.pods().inNamespace(namespace).withName(podName).delete();
        return ResultVo.renderOk().withRemark("手动删除副本成功！");
    }

    @Override
    public ResultVo pvcs(String namespace) {
        List<PersistentVolumeClaim> items = null;
        if (Strings.isNotEmpty(namespace)){ items = kubernetesClient.persistentVolumeClaims().inNamespace(namespace).list().getItems(); }
        else{ items = kubernetesClient.persistentVolumeClaims().list().getItems(); }
        if (items.isEmpty()) return new ResultVo().withRemark("查询不到信息");
        List<String> pvcs = new ArrayList<>();
        items.forEach(item ->{
            String name = item.getMetadata().getName();
            pvcs.add(name);
        });
        return ResultVo.renderOk(pvcs).withRemark("查询存储卷成功！");
    }

    @Override
    public ResultVo restart(String namespace, String appName) {
        ResultVo resultVo = this.pauseOne(namespace, appName);
        if(!resultVo.getCode().equals(200)){ return resultVo.withRemark("重启失败！"); }
        App one = appDao.findOne(namespace, appName);
        try{
            resultVo = this.updateOne(one);
        }catch (Exception e){
            e.printStackTrace();
            return ResultVo.renderErr().withRemark("重启失败！"+e.getMessage());
        }

        return ResultVo.renderOk().withRemark("重启成功！，服务器可能存在延迟");

    }

    @Override
    public io.fabric8.kubernetes.api.model.Service createService(App app) {
        io.fabric8.kubernetes.api.model.Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(app.getAppName())
                .withNamespace(app.getNamespace())
                .endMetadata()
                .withNewSpec()
                .withSelector(Collections.singletonMap("name", app.getAppName()))
                .withPorts(new ServicePortBuilder().withProtocol("TCP").withPort(app.getContainerPort()).withNewTargetPort(app.getContainerPort())
                        .withNodePort(app.getNodePort()).withName("port1").build())
                .withType(app.getServiceType().equalsIgnoreCase("INGRESS") ? "ClusterIP":app.getServiceType())
                .endSpec()
                .build();
        if (app.getServiceType().equalsIgnoreCase("INGRESS")){
            ingressService.addOneIng(app);
        }else {
            ingressService.delOneIng(app);
        }
        return service;
    }

    @Override
    public Deployment createDeployment(App app){
        ObjectMeta metadata = new ObjectMetaBuilder().withName(app.getAppName()).withNamespace(app.getNamespace()).build();
        DeploymentStrategy strategy = new DeploymentStrategyBuilder().withType(app.getUpdatePolicy()).withNewRollingUpdate().endRollingUpdate().build();
        List<Volume> volumes = null;
        List<VolumeMount> volumeMounts = null;
        if (Strings.isEmpty(app.getPvcNames())){
            volumes = new ArrayList<Volume>(){{
                add(new VolumeBuilder().withName(app.getAppName()).withEmptyDir(new EmptyDirVolumeSourceBuilder().build()).build());
            }};
        }else {
            volumeMounts = new ArrayList<VolumeMount>();
            volumes = new ArrayList<Volume>();
            String[] splitVolume = app.getPvcNames().split(",");
            String[] splitMountPath = app.getMountPaths().split(",");
            for (int i = 0; i < splitVolume.length; i++) {
                String volName = splitVolume[i];
                String mountPath = splitMountPath[i];
                volumes.add(new VolumeBuilder().withName(volName).withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(volName).build()).build());
                volumeMounts.add(new VolumeMountBuilder().withName(volName).withMountPath(mountPath).build());
            }
        }
        List<EnvVar> envs = new ArrayList<EnvVar>();
        String pattern = "\\w+(,\\w+)*";
        Pattern r = Pattern.compile(pattern);
        if (Strings.isNotEmpty(app.getEnvs())){
            EnvVar envVar = null;
            String[] split = app.getEnvs().split(";");
            for (String s : split) {
                if (!r.matcher(s).find()) continue;
                String value = "";
                String[] split1 = s.split(",");
                if (split1==null || split1.length<2) continue;
                String name = split1[0];
                value = split1[1];
                if(!MyUtil.isBase64(name) || !MyUtil.isBase64(value)){
                    throw new BizException(CodeEnum.ERR).withRemark("非法的参数请按照，base64,base64;base64,base64格式提交");
                }
                envVar = new EnvVarBuilder().withName(MyUtil.deCodeBase64(name)).withValue(MyUtil.deCodeBase64(value)).build();
                envs.add(envVar);
            }
        }

        Probe livenessProbe = null;
        if(Strings.isNotEmpty(app.getPointer())){
            IntOrString port = new IntOrString(app.getContainerPort());
            HTTPGetAction httpGetAction = new HTTPGetActionBuilder()
                    .withPath(app.getPointer())
                    .withPort(port)
                    .build();
            livenessProbe = new ProbeBuilder()
                    .withHttpGet(httpGetAction)
                    .withInitialDelaySeconds(120)
                    .withPeriodSeconds(10)
                    .build();
        }

        Container container = new ContainerBuilder()
                .withImage(app.getImageName())
                .withImagePullPolicy(app.getImagePolicy())
                .withName(app.getAppName())
                .withEnv(envs)
                .withLivenessProbe(livenessProbe)
                .withPorts(new ContainerPortBuilder().withContainerPort(app.getContainerPort()).build())
                .withNewResources()
                .withLimits(new HashMap<String, Quantity>() {{
                    put("cpu", new QuantityBuilder().withAmount(app.getCpuAmount()).withFormat(app.getCpuFormat()).build());
                    put("memory", new QuantityBuilder().withAmount(app.getMemoryAmount()).withFormat(app.getMemoryFormat()).build());
                }})
                .endResources()
                .withVolumeMounts(volumeMounts)
                .build();
        Map<String, String> nodeSelecor = new HashMap<String, String>(){{
            if (Strings.isNotEmpty(app.getNodeSelector())){
                String[] selectors = app.getNodeSelector().split(",");
                for(int i=0; i<selectors.length; i++) {
                    String key = selectors[i].split("=")[0];
                    String value = null;
                    if (selectors[i].split("=").length>1){
                        value = selectors[i].split("=")[1];
                    }
                    put(key, value);
                }}
        }};

        List<HostAlias> hostAliasList = new ArrayList<>();
        if (Strings.isNotEmpty(app.getDominName())){
            String dominName = app.getDominName();
            String[] domins = dominName.split(";");
            for (int i=0; i<domins.length; i++){
                List<String> ips = new ArrayList<>();
                if (!r.matcher(domins[i]).find()) continue;
                String[] domin = domins[i].split(",");
                if (domin==null || domin.length<2) continue;
                String hostIP = domin[0];
                Collections.addAll(ips, Arrays.copyOfRange(domin, 1, domin.length));
                HostAlias hostAlias = new HostAliasBuilder().withNewIp(hostIP).withHostnames(ips).build();
                hostAliasList.add(hostAlias);
            }
        }

        PodTemplateSpec template = new PodTemplateSpecBuilder()
                .withNewMetadata()
                .withLabels(new HashMap<String, String>(){{ put("name", app.getAppName()); }})
                .endMetadata()
                .withNewSpec()
                .withContainers(container)
                .withHostIPC(true)
                .withHostAliases(hostAliasList)
                .withNodeSelector(nodeSelecor)
                .withRestartPolicy("Always")
                .withVolumes(volumes)
                .endSpec().build();
        DeploymentSpec spec = new DeploymentSpecBuilder()
                .withMinReadySeconds(30)
                .withReplicas(app.getReplicas())
                .withSelector(new LabelSelectorBuilder().withMatchLabels(new HashMap<String, String>(){{
                    put("name",app.getAppName());
                }}).build())
                .withStrategy(strategy)
                .withTemplate(template)
                .build();
        Deployment deployment = new DeploymentBuilder()
                .withMetadata(metadata)
                .withSpec(spec)
                .withNewStatus()
                .withReplicas(app.getReplicas())
                .endStatus()
                .build();

        return deployment;
    }



}
