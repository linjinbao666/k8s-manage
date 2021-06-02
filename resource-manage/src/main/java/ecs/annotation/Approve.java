package ecs.annotation;

import java.lang.annotation.*;

/**
 * 审核注解
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Approve {
    /**
     * 用于标记是审核操作类
     * true表示发送审核
     * false表示不发送
     * @return
     */
    boolean value() default false;

    /**
     * 操作名称
     * @return
     */
    String operationName() default "";
}
