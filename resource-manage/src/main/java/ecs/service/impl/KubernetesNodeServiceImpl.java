package ecs.service.impl;

import ecs.dao.KubernetesNodeDao;
import ecs.dao.ResourceRequestDao;
import ecs.entity.KubernetesNode;
import ecs.entity.ResourceRequest;
import ecs.service.KubernetesNodeService;
import ecs.util.AtomicDouble;
import ecs.util.MyUtil;
import ecs.vo.K8sResourceVo;
import ecs.vo.ResultVo;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class KubernetesNodeServiceImpl implements KubernetesNodeService {
    private static final Logger LOG = LoggerFactory.getLogger(KubernetesNodeService.class);
    @Autowired
    private KubernetesNodeDao kubernetesNodeDao;

    @Autowired
    ResourceRequestDao resourceRequestDao;

    @Autowired
    KubernetesClient kubernetesClient;

    @Override
    public List<KubernetesNode> findAll(){
        /**
         * 此处存疑： 如何读出k8s的节点列表
         * 1. 从apiserver读取真实的
         * 2. 从服务器管理表中读出类型为k8s的服务器，但是可能与真实的不一致
         * 3. 此处选择从apiserver读取
         */
        NonNamespaceOperation<Node, NodeList, DoneableNode, Resource<Node, DoneableNode>> nodes = kubernetesClient.nodes();
        List<KubernetesNode> kubernetesNodes = new ArrayList<>();
        NodeList nodeList = nodes.list();
        List<Node> items = nodeList.getItems();
        items.forEach(item -> {
            KubernetesNode kubernetesNode = new KubernetesNode();
            List<NodeAddress> addresses = item.getStatus().getAddresses();
            for(NodeAddress address : addresses) {
                if(address.getType().equals("InternalIP")){
                    kubernetesNode.setIp(address.getAddress());
                }else {
                    kubernetesNode.setHostname(address.getAddress());
                }
            }
            Map<String, Quantity> allocatable = item.getStatus().getAllocatable();
            String cpu = allocatable.get("cpu").getAmount();
            kubernetesNode.setCpu(Integer.valueOf(cpu));
            String memory = allocatable.get("memory").getAmount();
            double rounding = rounding(Double.valueOf(memory) / 1000000);
            kubernetesNode.setMemory(rounding);
            String labels = item.getMetadata().getLabels().toString();
            String replace = labels.replace("{", "").replace("}", "");
            kubernetesNode.setLabels(replace);
            List<NodeCondition> lists = item.getStatus().getConditions();
            String status = "not ready";
            for (NodeCondition nodeCondition : lists) {
                if("Ready".equals(nodeCondition.getType())) {
                    status = nodeCondition.getStatus();
                    break;
                }
            }
            if (status.equals("True")){
                kubernetesNode.setStatus("在线");
            }else {
                kubernetesNode.setStatus("离线");
            }
            kubernetesNodes.add(kubernetesNode);
        });

        /**
         * @// TODO: 2020/8/20  需要写入数据库
         */

        kubernetesNodes.forEach(node ->{
            KubernetesNode tmp = kubernetesNodeDao.findByHostName(node.getHostname());
            if (null != tmp) {
                node.setId(tmp.getId());
                node.setCreateDate(tmp.getCreateDate());
                node.setUpdateDate(new Date());
            }
        });
        kubernetesNodeDao.saveAll(kubernetesNodes);
        return kubernetesNodes;
    }

    @Override
    public ResultVo getNodeDetail(String nodeName) {
        ResultVo resultVo = new ResultVo();
        KubernetesNode kubernetesNode = kubernetesNodeDao.findByHostName(nodeName);

        if (null == kubernetesNode){
            resultVo.setCode(201);
            resultVo.setMsg("节点不存在，可能由于数据未同步");
            return resultVo;
        }
        resultVo.setCode(200);
        resultVo.setMsg("查询节点详情成功!");
        resultVo.setData(kubernetesNode);
        return resultVo;
    }

    /**
     * 获取cpu和内存使用数据
     * @return
     */
    @Override
    public List<K8sResourceVo> getCpuAndMemoryInfo() {
        double cpuALl = 0.0;
        double memoryAll = 0.0;
        double cpuUsed = 0.0;
        double memoryUsed = 0.0;
        KubernetesClient client = kubernetesClient;

        /**
         * 计算一共的cpu和内存
         */
        List<Node> items = client.nodes().list().getItems();
        assert items!=null;
        for (Node node : items){
            long nodeCpu = 0l;
            long nodeMemory = 0l;
            Map<String, Quantity> allocatable = node.getStatus().getAllocatable();
            Quantity nodeCpuAll = allocatable.get("cpu");
            Quantity nodeMemoryAll = allocatable.get("memory");
            String nodeCpuAllFormat = nodeCpuAll.getFormat();
            String nodeMemoryAllFormat = nodeMemoryAll.getFormat();
            if (Strings.isEmpty(nodeCpuAllFormat)) {
                nodeCpu = Long.parseLong(nodeCpuAll.getAmount());
            }else {
                nodeCpu = Long.parseLong(nodeCpuAll.getAmount())/1000;
            }
            if (nodeMemoryAllFormat.equals("Gi")) {
                nodeMemory = Long.parseLong(nodeMemoryAll.getAmount());
            }else if (nodeMemoryAllFormat.equals("Mi")){
                nodeMemory = Long.parseLong(nodeMemoryAll.getAmount())/1024;
            }else if (nodeMemoryAllFormat.equals("Ki")){
                nodeMemory = Long.parseLong(nodeMemoryAll.getAmount())/1024/1024;
            }
            cpuALl += nodeCpu;
            memoryAll += nodeMemory;
        }


        /**
         * 计算已经分配出去的cpu和内存
         */

        List<ResourceRequest> requests = resourceRequestDao.findAll();
        assert  requests!=null;
        for (ResourceRequest request : requests){
            Integer cpuRequest = request.getCpuRequests();
            double memoryRequest = request.getMemoryRequests();
            cpuUsed += cpuRequest;
            memoryUsed += memoryRequest;
        }

        K8sResourceVo cpuVo = new K8sResourceVo();
        cpuVo.setName("cpu");
        cpuVo.setAll(cpuALl);
        cpuVo.setUsed(cpuUsed);
        cpuVo.setAvaliable(cpuALl-cpuUsed);
        cpuVo.setPercentage(cpuVo.getAvaliable()/cpuVo.getAll());

        K8sResourceVo memoryVo = new K8sResourceVo();
        memoryVo.setName("memory");
        memoryVo.setUsed(memoryUsed);
        memoryVo.setAll(memoryAll);
        memoryVo.setAvaliable(memoryAll-memoryUsed);
        memoryVo.setPercentage(memoryVo.getAvaliable()/ memoryVo.getAll());
        List list = new ArrayList();
        list.add(cpuVo);
        list.add(memoryVo);

        return list;
    }

    public double rounding(double data) {
        NumberFormat nf = new DecimalFormat( "0.00");
        data = Double.parseDouble(nf.format(data));
        return data;
    }

}

