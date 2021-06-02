package ecs.config;

import ecs.vo.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
@Slf4j
public class AuditorConfig implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        String operator = UserContext.getUserContext().getUser().getName();
        log.info("当前操作用户 : [{}]", null == operator ? "system" : operator);
        return Optional.ofNullable(null == operator ? "system" : operator);
    }
}
