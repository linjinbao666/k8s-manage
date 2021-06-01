package ecs.service;

import ecs.entity.EcsResource;
import ecs.vo.ResultVo;

import java.util.List;
import java.util.Map;

public interface EcsResourceService {
    List<EcsResource> findAll();

    ResultVo add(EcsResource ecsResource);

    ResultVo update(EcsResource ecsResource);

    ResultVo delete(Long id, String password);

    EcsResource findEcsResource(int status);

    List<EcsResource> find(Map<String, Object> params, Integer pageNum, Integer pageSize);

    long count(Map<String, Object> params);

    ResultVo findDetail(Long id);

    boolean verify(String ip, String password,Integer remotePort);

    /**
     * 基本信息
     * @param ip
     * @return
     */
    ResultVo basicInfo(String ip);

    /**
     * 进程列表
     * @param ip
     * @return
     */
    ResultVo processList(String ip);

    /**
     * 磁盘信息
     * @param ip
     * @return
     */
    ResultVo diskInfo(String ip);

    /**
     * 网卡信息
     * @param ip
     * @return
     */
    ResultVo ethInfo(String ip);

    /**
     * 端口信息
     * @param ip
     * @return
     */
    ResultVo portsListen(String ip);

    /**
     * cpu信息
     * @return
     */
    ResultVo cpuInfo(String ip, String rangeType);

    /**
     * 内存信息
     * @return
     */
    ResultVo memoryInfo(String ip, String rangeType);
}
