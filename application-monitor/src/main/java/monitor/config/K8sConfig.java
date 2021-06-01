package monitor.config;


import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class K8sConfig {

    @Value("${kubernetes.admin-conf}")
    String adminConf;

    @Bean
    Config config() throws IOException {
        File file = ResourceUtils.getFile(adminConf);
        FileInputStream inputStreamn = new FileInputStream(file);
        String adminConfData = IOUtils.toString(inputStreamn, "UTF-8");
        return Config.fromKubeconfig(adminConfData);
    }
    @Bean(name = "kubernetesClient")
    KubernetesClient kubernetesClient(Config config){
        log.info("初始化k8s配置");
        return new DefaultKubernetesClient(config);
    }
}
