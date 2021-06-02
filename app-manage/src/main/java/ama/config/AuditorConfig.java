package ama.config;

import ama.vo.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

/**
 * 用于jpa操作数据库时候自动添加操作用户
 */
@Slf4j
@Configuration
public class AuditorConfig implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        String operator = UserContext.getUserContext().getUser().getName();
        log.info("当前操作用户 : [{}]", operator);
        return Optional.ofNullable(operator);
    }
}
