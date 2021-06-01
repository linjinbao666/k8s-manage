package monitor.service;

import monitor.vo.K8sResourceVo;
import monitor.vo.ResultVo;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public interface ResourceService {

    /**
     * 获取cpu和内存总量
     * @return
     */
    List<K8sResourceVo> allCpuAndMemory();

    /**
     * 获取一段时间内的cpu使用率
     * @return
     */
    ResultVo getCpuRange(String appName, String containerName, String chronoUnit);


    /**
     * 获取一段时间的内存使用率
     * @return
     */
    ResultVo getMemoryRange(String appName, String containerName, String chronoUnit);


    /**
     * 获取一段时间的io使用率
     * @return
     */
    ResultVo getIORange(String appName, String containerName, String chronoUnit);

    /**
     * 获取应用列表
     * @return
     */
    ResultVo apps();

    /**
     * 根据应用名称查询容器
     * @param appName
     * @return
     */
    ResultVo containers(String namespace, String appName);

    ResultVo apps2();
}
