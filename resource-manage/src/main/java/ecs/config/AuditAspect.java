package ecs.config;

import ecs.vo.ResultVo;
import ecs.vo.UserContext;
import ecs.vo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

@Aspect
@Component
public class AuditAspect {
    @Pointcut("execution(public * ecs.service.*.*(..))")
    public void auditCut(){ }

    /**
     * Throwable是Error和Exception的父类，用来定义所有可以作为异常被抛出来的类。
     * <link href="https://blog.csdn.net/hl_java/article/details/76837141">
     * 全局异常处理类 {@link ecs.handler.GlobalExceptionHandler}
     *  <p>
     *      1. 使用Exception的超类Throwable，覆盖例如OOM异常
     *      2. 在finally中打印操作
     *      3. 不能使用throw Throwable，原因是：throw会抛出异常给调用者处理，如果是controller调用service，会被全局异常捕获，如果是定时任务则不能捕获；
     *      此处使用catch，再主动抛出
     *  </p>
     * @param joinPoint
     * @return
     */
    @Around("auditCut()")
    public Object around(ProceedingJoinPoint joinPoint) {
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass().getName()+"."+joinPoint.getSignature().getName());
        UserInfo user = UserContext.getUserContext().getUser();
        long startTimeMillis = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable te) {
            te.printStackTrace();
            throw new RuntimeException(te.getMessage());
        }finally {
            long execTimeMillis = System.currentTimeMillis() - startTimeMillis;
            log.info("[{}] [{}] [{}ms]",user.getId(), user.getName(), execTimeMillis);
        }
    }
}
