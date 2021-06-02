package ecs.service;

import ecs.entity.KubernetesNode;
import ecs.vo.K8sResourceVo;
import ecs.vo.ResultVo;

import java.util.List;

public interface KubernetesNodeService {
    List<KubernetesNode> findAll();

    ResultVo getNodeDetail(String nodeName);

    List<K8sResourceVo> getCpuAndMemoryInfo();
}
