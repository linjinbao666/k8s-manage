package ama.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class DockerConfig {

    @Value("${docker-java.dockerHost}")
    String dockerHost;
    @Bean(name = "dockerClientConfig")
    DockerClientConfig dockerClientConfig(){
        log.info("初始化docker配置" + dockerHost);
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(false)
                .build();
    }

    @Bean(name = "dockerHttpClient")
    DockerHttpClient dockerHttpClient(){
        log.info("初始化dockerHttpClient");
        return new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerClientConfig().getDockerHost())
                .sslConfig(null)
                .build();
    }

    @Bean(name = "dockerClient")
    DockerClient dockerClient(){
        log.info("初始化dockerClient");
      return DockerClientImpl.getInstance(dockerClientConfig(), dockerHttpClient());
    }

    @Value("${docker-java.registerUrl}")
    String registerUrl;
    @Value("${docker-java.username}")
    String username;
    @Value("${docker-java.password}")
    String password;

    @Bean(value = "authConfig")
    AuthConfig authConfig(){
        log.info("初始化authConfig");
        AuthConfig authConfig = new AuthConfig()
                .withRegistryAddress(registerUrl)
                .withUsername(username)
                .withPassword(password);
        log.info("检测dockerhub用户和密码");
        AuthResponse exec = dockerClient().authCmd().withAuthConfig(authConfig).exec();
        if (!exec.getStatus().contains("Login Succeeded")) { log.info("dockerhub用户密码检测未通过，请检查！"); }
        authConfig.withIdentityToken(exec.getIdentityToken());
        log.info("dockerhub用户密码检测通过，初始化完成");
        return authConfig;
    }

    public boolean ping() {
        try {
            CompletableFuture<Boolean> booleanCompletableFuture = CompletableFuture.supplyAsync(() -> {
                dockerClient().pingCmd().exec();
                return true;
            });
            return booleanCompletableFuture.get(3, TimeUnit.SECONDS);
        } catch (Throwable e) {
            return false;
        }
    }

}
