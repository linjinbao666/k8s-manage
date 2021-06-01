package ecs.handler;

import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.SftpException;
import ecs.dao.EcsResourceDao;
import ecs.dao.PvcDao;
import ecs.entity.EcsResource;
import ecs.entity.Pvc;
import ecs.util.SpringUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.*;

/**
 * @author linjinbao66@gmail.com
 * @date 2020/6/7
 * 处理文件socket
 */
public class BinaryHandler extends BinaryWebSocketHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BinaryHandler.class);
    private String fileOut;      //输出的文件名称
    private static String remoteFile;   //远程路径+文件
    private static Sftp sftp = null;

    @SneakyThrows
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        LOG.info("handleTextMessage");
        String payload = message.getPayload();
        LOG.info("payload = "+ payload);
        String[] split = payload.split(",");
        if (split.length != 3) {
            session.sendMessage(new TextMessage("请按照文件名称,存储卷名称,名称空间上传"));
            session.close();
            return;
        }
        fileOut = split[0];             //文件名称
        String pvcName = split[1];      //pvc名称
        String namespace = split[2];    //名称空间

        KubernetesClient client = SpringUtil.getBean(KubernetesClient.class);
        PvcDao pvcDao = SpringUtil.getBean(PvcDao.class);

        Pvc pvc = pvcDao.findByPvcName(namespace, pvcName);
        String pvname = pvc.getPvname();
        String server = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getServer();
        String path = client.persistentVolumes().withName(pvname).get().getSpec().getNfs().getPath();
        LOG.info("path = " + path);
        remoteFile = path+"/"+fileOut;

        EcsResourceDao ecsResourceDao = SpringUtil.getBean(EcsResourceDao.class);

        EcsResource ecsResource = ecsResourceDao.findByIp(server);
        try {
            if (ecsResource==null){
                session.sendMessage(new TextMessage("请检查是否已经录入了nfs主机信息:："+ server));
            }else if (pvc==null){
                session.sendMessage(new TextMessage("存储卷不存在："+ pvcName));
            }else {
                session.sendMessage(new TextMessage("开始传输文件："+ remoteFile));
            }
        } catch (IOException e) {
            session.sendMessage(new TextMessage("传输出错："+ e.getMessage()));
            e.printStackTrace();
        }

        if(sftp==null) {
            sftp = new Sftp(server, ecsResource.getRemotePort(), ecsResource.getAccount(), ecsResource.getPassword());
        }
    }
    @SneakyThrows
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        LOG.info("handleBinaryMessage");

        int payloadLength = message.getPayloadLength();
        try {
            session.sendMessage(new TextMessage("progress" + payloadLength));
        } catch (IOException e) {
            session.sendMessage(new TextMessage("传输出错："+ e.getMessage()));
            e.printStackTrace();
        }
        byte[] b = message.getPayload().array();
        InputStream sbs = new ByteArrayInputStream(b);
        try {
            sftp.getClient().put(sbs,remoteFile,2);
        } catch (SftpException e) {
            session.sendMessage(new TextMessage("传输出错："+ e.getMessage()));
            e.printStackTrace();
        }finally {
            sbs.close();
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) throws Exception {
        LOG.info("session = " + session);

        synchronized (session){
            if (webSocketMessage instanceof TextMessage){
                handleTextMessage(session, (TextMessage)webSocketMessage);
            }else if (webSocketMessage instanceof BinaryMessage){
                handleBinaryMessage(session, (BinaryMessage)webSocketMessage);
            }else if (webSocketMessage instanceof PongMessage) {
                handlePongMessage(session, (PongMessage) webSocketMessage);
            }else {
                throw new IllegalStateException("Unexpected WebSocket message type: " + webSocketMessage);
            }
        }

    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        LOG.info("handlePongMessage");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        exception.printStackTrace();
        session.close();
        LOG.info("handleTransportError  =======   ");
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public BinaryHandler() {
        super();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOG.info("连接建立，开始通信");
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        LOG.info("连接关闭，，，");
        session.close();
        sftp = null;
    }

}
