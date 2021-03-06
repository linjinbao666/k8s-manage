package monitor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class JSONTest {
    public static void main(String[] args) {
        String s = "{\"receiver\":\"webhook\",\"status\":\"firing\",\"alerts\":[{\"status\":\"firing\",\"labels\":{\"alertname\":\"test02\",\"device\":\"/dev/mapper/centos-root\",\"endpoint\":\"https\",\"fstype\":\"xfs\",\"instance\":\"*.16.12.21:9100\",\"job\":\"node-exporter\",\"mountpoint\":\"/\",\"namespace\":\"monitoring\",\"page\":\"monitoring\",\"pod\":\"node-exporter-gbzls\",\"prometheus\":\"monitoring/k8s\",\"service\":\"node-exporter\",\"team\":\"monitoring\"},\"annotations\":{\"description\":\"Check failing services\"},\"startsAt\":\"2020-09-23T11:58:39.594449831Z\",\"endsAt\":\"0001-01-01T00:00:00Z\",\"generatorURL\":\"http://prometheus-k8s-1:9090/graph?g0.expr=%28node_filesystem_size_bytes%7Bmountpoint%3D%22%2F%22%7D+-+node_filesystem_free_bytes%7Bmountpoint%3D%22%2F%22%7D%29+%2F+node_filesystem_size_bytes%7Bmountpoint%3D%22%2F%22%7D+%2A+100+%3E+67\\u0026g0.tab=1\"},{\"status\":\"firing\",\"labels\":{\"alertname\":\"test02\",\"device\":\"rootfs\",\"endpoint\":\"https\",\"fstype\":\"rootfs\",\"instance\":\"*.16.12.21:9100\",\"job\":\"node-exporter\",\"mountpoint\":\"/\",\"namespace\":\"monitoring\",\"page\":\"monitoring\",\"pod\":\"node-exporter-gbzls\",\"prometheus\":\"monitoring/k8s\",\"service\":\"node-exporter\",\"team\":\"monitoring\"},\"annotations\":{\"description\":\"Check failing services\"},\"startsAt\":\"2020-09-23T11:58:39.594449831Z\",\"endsAt\":\"0001-01-01T00:00:00Z\",\"generatorURL\":\"http://prometheus-k8s-1:9090/graph?g0.expr=%28node_filesystem_size_bytes%7Bmountpoint%3D%22%2F%22%7D+-+node_filesystem_free_bytes%7Bmountpoint%3D%22%2F%22%7D%29+%2F+node_filesystem_size_bytes%7Bmountpoint%3D%22%2F%22%7D+%2A+100+%3E+67\\u0026g0.tab=1\"}],\"groupLabels\":{\"job\":\"node-exporter\"},\"commonLabels\":{\"alertname\":\"test02\",\"endpoint\":\"https\",\"instance\":\"*.16.12.21:9100\",\"job\":\"node-exporter\",\"mountpoint\":\"/\",\"namespace\":\"monitoring\",\"page\":\"monitoring\",\"pod\":\"node-exporter-gbzls\",\"prometheus\":\"monitoring/k8s\",\"service\":\"node-exporter\",\"team\":\"monitoring\"},\"commonAnnotations\":{\"description\":\"Check failing services\"},\"externalURL\":\"http://alertmanager-main-2:9093\",\"version\":\"4\",\"groupKey\":\"{}:{job=\\\"node-exporter\\\"}\"}";

        JSONObject jsonObject = JSON.parseObject(s);
        JSONArray alerts = jsonObject.getJSONArray("alerts");

        JSONObject commonLabels = jsonObject.getJSONObject("commonLabels");
        String alertname = commonLabels.getString("alertname");
        System.out.println(alertname);

//        for (int i=0; i< alerts.size(); i++){
//            JSONObject jsonObject1 = alerts.getJSONObject(i);
//            String status = jsonObject1.getString("status");
//            System.out.println(status);
//        }

    }
}
