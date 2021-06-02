package ama.service;

import ama.entity.App;

public interface IngressService {
    /**
     * 增加一条新的ing规则
     */
    void addOneIng(App app);
    void delOneIng(App app);

    /**
     * 获取服务器部署的ing http端口
     * @return
     */
    int ingressHttpPort();

    /**
     * 返回目标应用已经存在的ing规则
     * @param app
     * @return
     */
    String findOneIng(App app);
}
