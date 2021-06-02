package ama.service;

import ama.entity.App;
import ama.vo.ResultVo;
import cn.hutool.json.JSONObject;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 发布一个应用的过程包含
 * 1. 发布deployment
 * 2. 发布service
 */

public interface AppService {

    /**
     * 同步所有应用信息到数据据
     * @param namespace
     */
    void syncApp(String namespace);

    /**
     * 同步一个应用信息到数据库
     * @param namespace
     * @param appName
     */
    void syncOne(String namespace, String appName);

    /**
     * 查询总数
     * @return
     */
    long count(Map<String, Object> params);

    /**
     * 查询所有应用
     * @return
     */
    List<App> findAll();

    /**
     * 分页查询
     * @param params
     * @param pageNum
     * @param pageSize
     * @return
     */
    List<App> findAll(Map<String, Object> params, Integer pageNum, Integer pageSize);

    /**
     * 查询单个应用详情
     * @param namespace
     * @param appName
     * @return
     */
    App findOne(String namespace, String appName);

    /**
     * 新增一个应用
     * @param app
     * @return
     */
    ResultVo addOne(App app);


    /**
     * 构建service 理解成proxy
     * @param app
     * @return
     */
    io.fabric8.kubernetes.api.model.Service createService(App app);

    /**
     * 构建deployment
     * @param app
     * @return
     */
    Deployment createDeployment(App app) throws Exception;

    /**
     * 删除一个应用
     * @param namespace
     * @param appName
     * @return
     */
    ResultVo deleteOne(String namespace, String appName);

    /**
     * 停止一个应用
     * @param namespace
     * @param appName
     * @return
     */
    ResultVo pauseOne(String namespace, String appName);

    /**
     * 更新应用
     * @param app
     * @return
     */
    ResultVo updateOne(App app) throws Exception;

    /**
     * 获取数据库中所有已使用的端口
     * @return
     */
    List<Integer> usedNodePorts();


    /**
     * 获取应用下的pod
     * @param namespace
     * @param appName
     * @return
     */
    ResultVo pods(String namespace, String appName);

    /**
     * 获取所有nodeSelectors
     * @return
     */
    Map nodeSelectors();

    /**
     * 查询日志
     * @return
     * @param namespace
     * @param podName
     * @param lines
     */
    InputStream podLog(String namespace, String podName, Integer lines);

    /**
     * 根据名称空间查询资源使用情况
     * @param namespace
     * @return
     */
    ResultVo resourceUsed(String namespace);

    /**
     * 自动扩容
     * @param namespace
     * @param appName
     * @param min 最小个数
     * @param max 最大个数
     * @param cpuPercentage cpu使用率
     * @param memoryPercentage 内存使用lv
     * @return
     */
    ResultVo hpa(String namespace, String appName,
                           Integer min, Integer max,
                           Integer cpuPercentage, Integer memoryPercentage);

    /**
     * 删除hpa
     * @param namespace
     * @param appName
     * @return
     */
    ResultVo deleteHpa(String namespace, String appName);
    /**
     * 手动删除副本
     * @param namespace
     * @param podName
     * @return
     */
    ResultVo deletePod(String namespace, String podName);

    /**
     * 获取存储卷
     * @param namespace
     * @return
     */
    ResultVo pvcs(String namespace);

    /**
     * 重启接口
     * @param namespace
     * @param appName
     * @return
     */
    ResultVo restart(String namespace, String appName);
}
