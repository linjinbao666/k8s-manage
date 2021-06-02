package ecs.service;

import ecs.entity.ResourceRequest;
import ecs.entity.SubCenter;
import ecs.vo.ResultVo;

import java.util.List;
import java.util.Map;

public interface ResourceRequestService {
    List<ResourceRequest> findAll(Map<String, Object> params);

    ResultVo sync();

    ResultVo createResourcesRequest(ResourceRequest resourceRequest);

    ResultVo deleteNamespace(String namespace);

    ResourceRequest findNamespace(String namespace);

    ResultVo updateResourcesRequest(ResourceRequest resourceRequest);

    /**
     * 创建分中心资源
     * @param subCenter
     * @return
     */
    ResultVo createSubCenter(SubCenter subCenter);

    ResultVo findSubCenter(String centerName);

    ResultVo deleteSubCenter(long id);
}
