package ecs.mq;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import ecs.annotation.Approve;
import ecs.service.ApprovalService;
import ecs.util.SpringUtil;
import ecs.vo.Approval;
import ecs.vo.ApprovalDetailVo;
import ecs.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;

import static ecs.util.MyUtil.changeAnnotationValue;

@Slf4j
@Component
@RabbitListener(queues = "FDMP_SYS_APPROVAL_RESOURCE_MANAGE")
public class RabbitConsumer {

    @Autowired
    private AmqpTemplate rabbitmqTemplate;
    @Autowired
    private ApprovalService approvalService;

    @RabbitHandler
    public void recieved(String msg, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(msg, JsonObject.class);
        Approval approval = gson.fromJson(jsonObject, Approval.class);
        log.info("[ecs] 接收到数据 , {} " , approval);

        String approvalKey = approval.getApprovalKey();
        String detailMessage = approval.getDetailMessage();
        String objectMemo = approval.getObjectMemo();
        String className = objectMemo.substring(0, objectMemo.lastIndexOf("."));
        String methodName = objectMemo.substring(objectMemo.lastIndexOf(".")+1, objectMemo.length());
        String beanName = className.substring(className.lastIndexOf(".")+1, className.length());

        ApprovalDetailVo detailVo = gson.fromJson(detailMessage, ApprovalDetailVo.class);
        String paramType = detailVo.getParamType();
        String paramsJsonStr = detailVo.getParamsJsonStr();
        ListParameterizedType type = null;
        try {
            type = new ListParameterizedType(Class.forName(paramType));
        } catch (ClassNotFoundException e) {
        }
        Object javaObject =  gson.fromJson(paramsJsonStr, type);
        Class<?> aClass = null;
        Annotation annotation = null;
        try {
            aClass = Class.forName(className);
            Class<?> clazz  = SpringUtil.getBean(aClass).getClass();  //获取到类
            Method method = clazz.getDeclaredMethod(methodName, Class.forName(paramType));   //获取到具体方法
            annotation = AnnotationUtils.findAnnotation(method, Approve.class);      //获取到注解
            changeAnnotationValue(annotation, "value", false);                  //修改注解
            Object invoke = method.invoke(SpringUtil.getBean(toLowerCaseFirstOne(beanName)), ((ArrayList) javaObject).get(0));
            log.info("invoke, {}", invoke);
            if (invoke instanceof ResultVo){
                if (((ResultVo<?>) invoke).isOk()) approvalService.sendResult(approvalKey, invoke.toString(), "true");
                else approvalService.sendResult(approvalKey, invoke.toString(), "false");
            }
        } catch (Exception e) {
            approvalService.sendResult(approvalKey, e.getMessage(), "false");
        }
        changeAnnotationValue(annotation, "value", true);                   //恢复注解

        try {
            channel.basicAck(tag,false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * spring托管的bean默认首字母小写
     * @param s
     * @return
     */
    public static String toLowerCaseFirstOne(String s) {
        if(Character.isLowerCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
    }

    /**
     * 修正转json过程中的泛型擦除问题
     * @see java.lang.reflect.ParameterizedType
     */
    private static class ListParameterizedType implements ParameterizedType {
        private Type type;
        private ListParameterizedType(Type type) {
            this.type = type;
        }
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[] {type};
        }
        @Override
        public Type getRawType() {
            return ArrayList.class;
        }
        @Override
        public Type getOwnerType() {
            return null;
        }

    }
}
