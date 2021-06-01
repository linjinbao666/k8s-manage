package ama;

import ama.entity.App;
import ama.service.AppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

@SpringBootApplication
@EnableJpaAuditing
@EnableSwagger2
@EnableScheduling
@Slf4j
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    AppService appService;

    @Scheduled(fixedRate = 1000*60*15)
    public void syncAppStatus() {
        List<App> all = appService.findAll();
        for (App app : all) {
            String namespace = app.getNamespace();
            String appName = app.getAppName();
            appService.syncOne(namespace, appName);
        }
    }


}