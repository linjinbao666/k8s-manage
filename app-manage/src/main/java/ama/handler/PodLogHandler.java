package ama.handler;

import ama.util.MyUtil;
import ama.util.SpringUtil;
import ama.util.TimeUtil;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.directory.api.util.Strings;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * pod日志
 */
@Slf4j
public class PodLogHandler extends TextWebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        KubernetesClient kubernetesClient = SpringUtil.getBean(KubernetesClient.class);
        String playload = message.getPayload();
        String[] split = playload.split(",");
        if (split.length != 2) {
            session.sendMessage(new TextMessage("请提供名称空间和podName"));
            session.close();
            return;
        }
        String namespace = split[0];
        String podName = split[1];
        session.sendMessage(new TextMessage("您的参数, namespace = "+ namespace +"podName="+podName));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String startTime = sdf.format(new Date());

        LogWatch logWatcher = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(podName)
                .sinceTime(startTime)
                .watchLog();
        InputStream in = logWatcher.getOutput();

        new Thread() {
            @Override
            public void run() {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    while (true) {
                        String line = reader.readLine();
                        if (Strings.isEmpty(line)) {
                            session.sendMessage(new TextMessage("waiting..."));
                        }else {
                            session.sendMessage(new TextMessage(line));
                        }
                    }
                } catch (IOException e) {
                    return;
                }
            }
        }.start();
    }

}
