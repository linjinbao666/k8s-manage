package ecs.util;

import cn.hutool.Hutool;
import cn.hutool.core.net.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class LinuxUtil {
    private static final Logger LOG = LoggerFactory.getLogger(LinuxUtil.class);
    /**
     * 判断能否ping通
     * @param ip
     * @return
     * @throws Exception
     */
    public static boolean ping(String ip) throws Exception {
        LOG.info("使用ping命令检测目标主机" + ip);
        int  timeOut =  3000;
        boolean status =  InetAddress.getByName(ip).isReachable(timeOut);
        return status;
    }

    /**
     * 使用socket方式检测主机
     * @param ip
     * @param port
     * @return
     */
    public static boolean socket(String ip, Integer port){
        LOG.info("使用scoket检测目标主机" + ip + ":" + port);
        int  timeOut =  3000;
        InetSocketAddress address = new InetSocketAddress(ip, port);
        boolean open = NetUtil.isOpen(address,timeOut);
        return open;
    }

    /**
     * ip校验
     * @param ip
     */
    public static boolean ipV4Verify(String ip){
        String regIp = "^([1-9]|[1-9]\\d|1\\d{2}|2[0-1]\\d|22[0-3])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}$";
        return ip.matches(regIp);
    }
}
