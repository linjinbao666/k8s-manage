package monitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
public class AuditorConfig implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        /**
         * 用户信息需要从header中取出
         * @// TODO: 2020/9/1
         */
        return Optional.of("admin");
    }
}
