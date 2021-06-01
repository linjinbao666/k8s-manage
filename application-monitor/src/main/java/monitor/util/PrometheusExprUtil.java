package monitor.util;

public class PrometheusExprUtil {
    public static String getExpr(String alertType, Double alertRate,String appName) {
        String expression = "";
        String cpuType = "cpu";
        String memoryType = "memory";
        String nodeType = "node";
        String diskType = "disk";
        String appType = "appExist";
        String diskIoReadType = "diskIoRead";
        String diskIoWriteType = "diskIoWrite";
        String netIoReadType = "netIoRead";
        String netIoWriteType = "netIoWrite";
        if (cpuType.equals(alertType)) {
            expression = "(100 - (avg by (instance) (rate(node_cpu_seconds_total{kubernetes_name=\"node-exporter\",mode=\"idle\"}[1m]) * 100)))" + ">" + alertRate;
        } else if (memoryType.equals(alertType)) {
            expression = "(node_memory_MemTotal_bytes- (node_memory_MemFree_bytes+node_memory_Buffers_bytes+node_memory_Cached_bytes)) / node_memory_MemTotal_bytes * 100" + ">" + alertRate;
        } else if (nodeType.equals(alertType)) {
            expression = "up==0";
        } else if(diskType.equals(alertType)) {
            expression = "(node_filesystem_size_bytes {mountpoint =\"/\"} - node_filesystem_free_bytes {mountpoint =\"/\"})/node_filesystem_size_bytes {mountpoint =\"/\"} * 100"+">"+ alertRate;
        } else if(appType.equals(alertType)) {
            expression = "sum(kube_pod_container_status_running{pod=~"+"\""+appName+".*$\"})by(pod)"+"< 1";
        } else if(netIoReadType.equals(alertType)) {
            //网络流量接收字节
            expression = "sum(rate(container_network_receive_bytes_total{id!=\"/\",instance=~\"^.*$\"}[1m]))by(instance)/1024" + ">" + alertRate*1024;
        } else if(netIoWriteType.equals(alertType)) {
            //网络流量上传字节
            expression = "sum(rate(container_network_transmit_bytes_total{id!=\"/\",instance=~\"^.*$\"}[5m]))by(instance)/1024" + ">" + alertRate*1024;
        } else if(diskIoReadType.equals(alertType)) {
            expression = "sum(irate(node_disk_read_bytes_total{instance=~\"^.*$\",device=~\"[a-z]*[a-z]\"}[1m]))by(instance)/1024" + ">" + alertRate*1024;
        } else if(diskIoWriteType.equals(alertType)) {
            expression = "sum(irate(node_disk_written_bytes_total{instance=~\"^.*$\",device=~\"[a-z]*[a-z]\"}[1m]))by(instance)/1024" + ">" + alertRate*1024;
        }
        return expression;
    }

}
