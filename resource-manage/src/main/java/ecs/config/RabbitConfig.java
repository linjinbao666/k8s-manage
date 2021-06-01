package ecs.config;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private final static String STRING = "FDMP_SYS_APPROVAL_RESOURCE_MANAGE";//子系统的队列

    public final static String TOPIC_EXCHANGE = "FDMP_SYS_APPROVAL";

    @Bean
    public Queue ecs() {
        return new Queue(STRING);
    }
    @Bean
    DirectExchange exchange(){
        return new DirectExchange(TOPIC_EXCHANGE);
    }

    @Bean
    Binding bindingExchange(Queue queue, DirectExchange directExchange){
        return BindingBuilder.bind(queue).to(directExchange).with("resource-center");
    }

}
