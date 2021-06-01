package ama;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.AuthResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.SearchItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DockerTest {
    public static void main(String[] args) throws Exception {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://10.20.250.21:2375")
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();
        DockerClient dockerClient = DockerClientImpl
                .getInstance(config, httpClient);
//        InputStream inputStream = Files.newInputStream(Paths.get("D:\\tmp\\busybox.tar"));
//        dockerClient.loadImageCmd(inputStream).exec();
//        Thread.sleep(5000);

        ExposedPort[] exposedPorts = dockerClient.inspectImageCmd("mysql:latest").exec().getContainerConfig().getExposedPorts();
        System.out.println(exposedPorts.length);
        for(ExposedPort port : exposedPorts){
            System.out.println(port.getPort());
        }


//        AuthConfig authConfig = new AuthConfig()
//                .withRegistryAddress("172.16.12.21/library")
//                .withUsername("admin")
//                .withPassword("Admin@harbor2020");

//        AuthResponse exec = dockerClient.authCmd().withAuthConfig(authConfig).exec();
//        System.out.println(exec.getStatus());
//        authConfig.withIdentityToken(exec.getIdentityToken());

//        dockerClient.tagImageCmd("edabd795951a",
//                "172.16.12.21/library/busybox", "0.0.1").exec().wait(3000);

//        boolean nginx = dockerClient
//                .pushImageCmd("busybox")
//                .withAuthConfig(authConfig)
//                .withName("172.16.12.21/library/busybox")
//                .withTag("0.0.1")
//                .start()
//                .awaitCompletion(20, TimeUnit.SECONDS);
//        System.out.println(nginx);

//        boolean b = dockerClient.pullImageCmd("nginx:1.7.9")
//                .withRegistry("172.16.12.21")
//                .start()
//                .awaitCompletion(30, TimeUnit.SECONDS);
//        List<SearchItem> nginx =
//                dockerClient.searchImagesCmd("nginx").withTerm("1.7.9").exec();
//        System.out.println(b);

    }
}
