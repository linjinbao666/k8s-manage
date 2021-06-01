package ecs.config;


import com.google.gson.Gson;
import ecs.annotation.Approve;
import ecs.service.ApprovalService;
import ecs.vo.Approval;
import ecs.vo.ApprovalDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
public class ApproveAspect {

    @Autowired
    ApprovalService approvalService;

    @Pointcut("@annotation(ecs.annotation.Approve)")
    public void pointcut(){ }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws ClassNotFoundException {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getMethod().getDeclaringClass().getName();

        Approve annotation = signature.getMethod().getAnnotation(Approve.class);
        if (!annotation.value()){
            try {
                Object proceed = joinPoint.proceed();
                return proceed;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        Approval approval = new Approval();
        approval.setBusinessType("resource-center");
        approval.setObjectName(annotation.operationName());  //注解上的操作名称
        approval.setObjectMemo(signature.getMethod().getDeclaringClass().getName()+"."+methodName); //类全名+方法名
        Gson gson = new Gson();
        List<Object> objects = Arrays.asList(args);
        ApprovalDetailVo detailVo = new ApprovalDetailVo(className,methodName,
                args[0].getClass().getTypeName(), gson.toJson(objects));
        approval.setDetailMessage(gson.toJson(detailVo, ApprovalDetailVo.class));
        return approvalService.send2Approval(approval);
    }
}
