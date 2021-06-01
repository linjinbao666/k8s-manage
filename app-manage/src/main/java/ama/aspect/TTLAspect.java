package ama.aspect;

import ama.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author linjb
 * @date 2020-09-02
 * 切面记录接口响应时常
 * 只需要环绕切面，记录方法开始执行的时间和执行之后的时间，放到返回结果中。
 *
 */

@Slf4j
@Aspect
@Component
public class TTLAspect {
    @Pointcut("execution(public * ama.controller.*.*(..))")
    public void ttlCut(){
    }

    @Around("ttlCut()")
    public Object around(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        try {
            log.info("方法 : [" + methodName + "] ---> 开始");
            long startTimeMillis = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long execTimeMillis = System.currentTimeMillis() - startTimeMillis;
            log.info("方法 : [" + methodName + "] ---> 执行时间 : " + execTimeMillis);
            log.info("方法 : [" + methodName + "] ---> 结束");
            if (result instanceof ResultVo){
                return ((ResultVo<?>) result).withTTL(execTimeMillis);
            }
            return result;
        } catch (Throwable te) {
            log.error(te.getMessage(),te);
            throw new RuntimeException(te.getMessage());
        }
    }
}
