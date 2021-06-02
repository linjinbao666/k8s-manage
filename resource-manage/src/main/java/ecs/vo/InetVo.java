package ecs.vo;

import java.io.Serializable;

/**
 * 网卡信息vo类
 */
public class InetVo implements Serializable {
    private String inet;
    private String netmask;
    private String broadcast;

    public String getInet() {
        return inet;
    }

    public void setInet(String inet) {
        this.inet = inet;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(String broadcast) {
        this.broadcast = broadcast;
    }
}
