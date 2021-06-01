package ecs.service.impl;

import ecs.annotation.Approve;
import ecs.dao.KubernetesNodeDao;
import ecs.dao.ResourceRequestDao;
import ecs.dao.SubCenterDao;
import ecs.entity.EcsResource;
import ecs.entity.ResourceRequest;
import ecs.entity.SubCenter;
import ecs.service.KubernetesNodeService;
import ecs.service.ResourceRequestService;
import ecs.vo.*;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

@Service
public class ResourceRequestServiceImpl implements ResourceRequestService {
    @Autowired
    ResourceRequestDao resourceRequestDao;

    @Autowired
    KubernetesNodeDao kubernetesNodeDao;

    @Autowired
    KubernetesNodeService kubernetesNodeService;

    @Autowired
    KubernetesClient client;

    @Override
    public List<ResourceRequest> findAll(Map<String, Object> params){
//        UserInfo user = UserContext.getUserContext().getUser();
//        Integer userType = user.getUserType();
//        if (userType.equals(1)){
//            return resourceRequestDao.findAll();
//        }else if(userType.equals(2)){
//            return resourceRequestDao.findAllByRegId(user.getUnit());
//        }
        Specification<ResourceRequest> specification = new Specification<ResourceRequest>() {
            @Override
            public Predicate toPredicate(Root<ResourceRequest> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> list = new ArrayList<Predicate>();
                params.forEach((key, value) -> { list.add(criteriaBuilder.equal(root.get(key).as(String.class), value)); });
                return criteriaQuery.where(list.toArray(new Predicate[list.size()])).getRestriction();
            }
        };
        return resourceRequestDao.findAll(specification);
    }

    /**
     * 主动同步
     */
    @Override
    public ResultVo sync(){
        List<ResourceRequest> requests =  new ArrayList<>();
        client.resourceQuotas().list().getItems().forEach(item ->{
            String namespace = item.getMetadata().getNamespace();
            Map<String, Quantity> hard = item.getSpec().getHard();
            ResourceRequest resourceRequest = new ResourceRequest();
            resourceRequest.setCompanyName(namespace);
            resourceRequest.setNamespace(namespace);
            resourceRequest.setCpuRequests(Integer.valueOf(hard.get("cpu").getAmount()));
            resourceRequest.setMemoryRequests(Double.parseDouble(hard.get("memory").getAmount()));
            requests.add(resourceRequest);
        });
        for (int i=0; i< requests.size(); i++){
            ResourceRequest byNamespace = resourceRequestDao.findByNamespace(requests.get(i).getNamespace());
            if (null != byNamespace) {
                requests.set(i, byNamespace);
            }
        }

        resourceRequestDao.saveAll(requests);
        return ResultVo.renderOk().withRemark("同步namespace成功");
    }

    @Approve(value = true, operationName = "申请厂商名称空间")
    @Override
    public ResultVo createResourcesRequest(ResourceRequest resourceRequest) {
        ResourceRequest tmp = resourceRequestDao.findByOrgOrNamespace(resourceRequest.getRegId(),
                resourceRequest.getNamespace());
        ResultVo resultVo = new ResultVo();
        if (Strings.isEmpty(resourceRequest.getNamespace()) || null!=tmp){
            resultVo.setCode(201);
            resultVo.setMsg("该组织下名称空间已经存在！组织名称：" + resourceRequest.getRegName());
            return resultVo;
        }
        List<Namespace> items = client.namespaces().list().getItems();
        for (int i=0; i< items.size(); i++){
            if (items.get(i).getMetadata().getName().equals(resourceRequest.getNamespace())){
                return ResultVo.renderErr(CodeEnum.ERR).withRemark("名称空间已经存在，请重新设置");
            }
        }

        SubCenter subCenter = null;
        if (null!=resourceRequest.getSubId() && 0 != resourceRequest.getSubId()){
            Optional<SubCenter> byId = subCenterDao.findById(resourceRequest.getSubId());
            if (!byId.isPresent()){
                return ResultVo.renderErr(CodeEnum.ERR).withRemark("分配失败，分中心不存在");
            }else {
                subCenter = byId.get();
                subCenter.setCpuUsed(subCenter.getCpuUsed() + resourceRequest.getCpuRequests());
                subCenter.setMemoryUsed(subCenter.getMemoryUsed()+resourceRequest.getMemoryRequests());
                subCenter.setDiskUsed(subCenter.getDiskUsed()+resourceRequest.getDiskRequests());
            }
        }

        double allCpu = 0.0;
        double allMemory = 0d;
        List<K8sResourceVo> list = kubernetesNodeService.getCpuAndMemoryInfo();
        for (int i=0; i< list.size(); i++){
            if(list.get(i).getName().equals("cpu")){
                allCpu = list.get(i).getAll();
            }else {
                allMemory = list.get(i).getAll();
            }
        }
        List<ResourceRequest> resourceRequestList = resourceRequestDao.findAll();
        long allocatedCpu = 0l;
        long allocatedMemory = 0l;
        for (int i=0; i< resourceRequestList.size(); i++){
            allocatedCpu +=resourceRequestList.get(i).getCpuRequests();
            allocatedMemory += resourceRequestList.get(i).getMemoryRequests();
        }

        if (resourceRequest.getCpuRequests() > (allCpu-allocatedCpu)
                || resourceRequest.getMemoryRequests() > (allMemory-allocatedMemory)){
            resultVo.setCode(-1);
            resultVo.setMsg("资源不足，无法分配！");
            return resultVo;
        }

        /**
         * @// TODO: 2020/8/20 真实分配名称空间
         */
        Quantity cpuliQn = new QuantityBuilder().withAmount(Double.toString(resourceRequest.getCpuRequests())).build();
        Quantity memliQn = new QuantityBuilder().withAmount(Double.toString(resourceRequest.getMemoryRequests())).withFormat("Gi").build();
        Map<String, Quantity> limits = new HashMap<>(16);
        limits.put("cpu", cpuliQn);
        limits.put("memory", memliQn);
        String namespace = resourceRequest.getNamespace();

        Namespace item = new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build();
        ResourceQuota resourceQuota = client.resourceQuotas().inNamespace(namespace).withName(namespace).get();
        if (resourceQuota != null) {
            client.resourceQuotas().inNamespace(namespace).withName(namespace).delete();
        }
        // 设置配额
        ResourceQuota quota = new ResourceQuotaBuilder().withNewMetadata().withName(namespace).withNamespace(namespace)
                .endMetadata().withNewSpec().withHard(limits).endSpec().build();
        Namespace namespaces = client.namespaces().withName(namespace).get();
        if (namespaces != null) {
            ResourceQuota quota2 = client.resourceQuotas().create(quota);
        } else {
            namespaces = client.namespaces().withName(namespace).create(item);
            ResourceQuota quota3 = client.resourceQuotas().create(quota);
        }

        resultVo.setCode(200);
        resultVo.setMsg("分配成功！");

        if (subCenter!=null) {
            subCenterDao.save(subCenter);
        }
        resourceRequestDao.save(resourceRequest);
        return resultVo;
    }

    @Transactional
    @Override
    public ResultVo deleteNamespace(String namespace){

//        UserInfo user = UserContext.getUserContext().getUser();
//        Integer userType = user.getUserType();
//        if (userType.equals(3) || null == userType) return ResultVo.renderErr(CodeEnum.ACCESS_DENIED);

        ResourceRequest byNamespace = resourceRequestDao.findByNamespace(namespace);
        if (null == byNamespace){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("名称空间不存在");
        }

        SubCenter subCenter = null;
        if (0 != byNamespace.getRegId()){
            Optional<SubCenter> byId = subCenterDao.findById(byNamespace.getRegId());
            if (byId.isPresent()) {
                subCenter = byId.get();
                subCenter.setCpuUsed(subCenter.getCpuUsed()-byNamespace.getCpuRequests());
                subCenter.setMemoryUsed(subCenter.getMemoryUsed()-byNamespace.getMemoryRequests());
                subCenter.setDiskUsed(subCenter.getDiskUsed()-byNamespace.getDiskRequests());
            }
        }

        ResultVo resultVo = new ResultVo();
        if (namespace.equals("default") ||
                namespace.equalsIgnoreCase("system") ||
                Strings.isEmpty(namespace) ||
                namespace.equals("kube-system")||
                namespace.equals("kube-public") ||
                namespace.equals("fline")){
            resultVo.setCode(201);
            resultVo.setMsg("禁止删除默认空间，删除失败！");
            return resultVo;
        }

        Boolean delete = client.namespaces().withName(namespace).delete();

        if (null!=subCenter) {
            subCenterDao.save(subCenter);
        }

        resourceRequestDao.deleteByNamespace(namespace);
        resourceRequestDao.flush();

        return ResultVo.renderOk().withRemark("删除厂商空间成功");
    }

    @Override
    public ResourceRequest findNamespace(String namespace) {
        ResourceRequest byNamespace = resourceRequestDao.findByNamespace(namespace);
        return byNamespace;
    }

    @Approve(value = true, operationName = "修改厂商名称空间")
    @Override
    public ResultVo updateResourcesRequest(ResourceRequest resourceRequest) {
//        UserInfo user = UserContext.getUserContext().getUser();
//        Integer userType = user.getUserType();
//        if (userType.equals(3)) return ResultVo.renderErr(CodeEnum.ACCESS_DENIED);
//        resourceRequest.setRegId(user.getUnit());

        String namespace = resourceRequest.getNamespace();
        ResourceRequest byNamespace =  resourceRequestDao.findByOrgAndNamespace(resourceRequest.getRegId(),
                namespace);
        if (null == byNamespace) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("名称空间不存在");
        }
        resourceRequest.setId(byNamespace.getId());

        double allCpu = 0.0;
        double allMemory = 0d;
        List<K8sResourceVo> list = kubernetesNodeService.getCpuAndMemoryInfo();
        for (int i=0; i< list.size(); i++){
            if(list.get(i).getName().equals("cpu"))allCpu = list.get(i).getAll();
            else allMemory = list.get(i).getAll();
        }

        List<ResourceRequest> resourceRequestList = resourceRequestDao.findAll();
        long allocatedCpu = 0l, allocatedMemory = 0l;
        for (int i=0; i< resourceRequestList.size(); i++){
            allocatedCpu +=resourceRequestList.get(i).getCpuRequests();
            allocatedMemory += resourceRequestList.get(i).getMemoryRequests();
        }
        /**调整计算方式**/
        allCpu = allCpu+byNamespace.getCpuRequests();
        allMemory = allMemory + byNamespace.getMemoryRequests();

        if (resourceRequest.getCpuRequests() > (allCpu-allocatedCpu)
                || resourceRequest.getMemoryRequests() > (allMemory-allocatedMemory)){
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("资源不足，无法分配！");
        }

        SubCenter subCenter = null;
        if (null!=resourceRequest.getSubId() && 0 != resourceRequest.getSubId()){
            Optional<SubCenter> byId = subCenterDao.findById(resourceRequest.getSubId());
            if (!byId.isPresent()){
                return ResultVo.renderErr(CodeEnum.ERR).withRemark("分配失败，分中心不存在");
            }else {
                subCenter = byId.get();
                subCenter.setCpuUsed(subCenter.getCpuUsed() + resourceRequest.getCpuRequests() - byNamespace.getCpuRequests());
                subCenter.setMemoryUsed(subCenter.getMemoryUsed()+resourceRequest.getMemoryRequests() -byNamespace.getMemoryRequests());
                subCenter.setDiskUsed(subCenter.getDiskUsed()+resourceRequest.getDiskRequests() -byNamespace.getDiskRequests());
            }
        }

        /**
         * 真实扩展空间
         */
        Quantity cpuliQn = new QuantityBuilder().withAmount(Double.toString(resourceRequest.getCpuRequests())).build();
        Quantity memliQn = new QuantityBuilder().withAmount(Double.toString(resourceRequest.getMemoryRequests())).withFormat("Gi").build();
        Map<String, Quantity> limits = new HashMap<>(16);
        limits.put("cpu", cpuliQn);
        limits.put("memory", memliQn);
        Namespace item = new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build();
        ResourceQuota resourceQuota = client.resourceQuotas().inNamespace(namespace).withName(namespace).get();
        if (resourceQuota != null) {
            client.resourceQuotas().inNamespace(namespace).withName(namespace).delete();
        }
        ResourceQuota quota = new ResourceQuotaBuilder().withNewMetadata().withName(namespace).withNamespace(namespace)
                .endMetadata().withNewSpec().withHard(limits).endSpec().build();
        Namespace namespaces = client.namespaces().withName(namespace).get();

        if (namespaces != null) {
            ResourceQuota quota2 = client.resourceQuotas().createOrReplace(quota);
        } else {
            namespaces = client.namespaces().withName(namespace).createOrReplace(item);
            ResourceQuota quota3 = client.resourceQuotas().createOrReplace(quota);
        }

        if (null!=subCenter) subCenterDao.save(subCenter);
        resourceRequestDao.save(resourceRequest);
        return ResultVo.renderOk().withRemark("扩展成功！");

    }

    @Autowired
    SubCenterDao subCenterDao;
    @Override
    public ResultVo createSubCenter(SubCenter subCenter) {
        double allCpu = 0.0;
        double allMemory = 0d;
        List<K8sResourceVo> list = kubernetesNodeService.getCpuAndMemoryInfo();
        for (int i=0; i< list.size(); i++){
            if(list.get(i).getName().equals("cpu")){
                allCpu = list.get(i).getAll();
            }else {
                allMemory = list.get(i).getAll();
            }
        }
        List<SubCenter> allCenter = subCenterDao.findAll();
        Integer usedcpu = 0;
        double usedMemory = 0.0;
        double usedDisk = 0.0;
        for (SubCenter center : allCenter) {
            usedcpu += center.getCpuRequest();
            usedMemory += center.getMemoryRequest();
            usedDisk += center.getDiskRequest();
        }
        if (allCpu < usedcpu+subCenter.getCpuRequest() || allMemory <usedMemory+subCenter.getMemoryRequest()) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("分中心申请资源失败，资源不足");
        }

        SubCenter subCenter1 = subCenterDao.findByName(subCenter.getCenterName());
        if (subCenter1!=null) {
            subCenter.setId(subCenter1.getId());
            SubCenter save = subCenterDao.save(subCenter);
            return ResultVo.renderOk(save).withRemark("修改分中心资源成功！");
        }
        SubCenter save = subCenterDao.save(subCenter);
        return ResultVo.renderOk(save).withRemark("申请分中心资源成功！");
    }

    @Override
    public ResultVo findSubCenter(String centerName) {
        if (Strings.isNotEmpty(centerName)) {
            SubCenter byName = subCenterDao.findByName(centerName);
            return ResultVo.renderOk(byName).withRemark("查询成功");
        }

        List<SubCenter> all = subCenterDao.findAll();
        return ResultVo.renderOk(all).withRemark("查询列表成功！");
    }

    @Override
    public ResultVo deleteSubCenter(long id) {

        boolean b = subCenterDao.existsById(id);
        if (!b) return ResultVo.renderErr(CodeEnum.ERR).withRemark("目标记录不存在");

        ResourceRequest bySubCenter = resourceRequestDao.findBySubCenter(id);
        if (null!=bySubCenter) return ResultVo.renderErr(CodeEnum.ERR).withRemark("分中心下存在厂商资源，禁止删除");

        subCenterDao.deleteById(id);

        return ResultVo.renderOk().withRemark("删除分中心成功");
    }
}
