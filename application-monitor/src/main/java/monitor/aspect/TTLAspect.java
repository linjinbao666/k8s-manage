package monitor.aspect;

import lombok.extern.slf4j.Slf4j;
import monitor.vo.ResultVo;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
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
    @Pointcut("execution(public * monitor.controller.*.*(..))")
    public void ttlCut(){
    }

    @Around("ttlCut()")
    public Object around(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        try {
            log.info("Method Name : [" + methodName + "] ---> AOP around start");
            long startTimeMillis = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long execTimeMillis = System.currentTimeMillis() - startTimeMillis;
            log.info("Method Name : [" + methodName + "] ---> AOP method exec time millis : " + execTimeMillis);
            log.info("Method Name : [" + methodName + "] ---> AOP around end");
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
