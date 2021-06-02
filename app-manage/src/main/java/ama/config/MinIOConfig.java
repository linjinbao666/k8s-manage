package ama.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * minio
 */

@Slf4j
@Configuration
public class MinIOConfig {

    @Value("${minio.url}")
    private String url;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean(name = "minioClient")
    public MinioClient minioClient(){
        log.info("初始化minioclient");
        return MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).build();
    }
}
