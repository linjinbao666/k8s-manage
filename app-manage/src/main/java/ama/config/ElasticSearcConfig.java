package ama.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * es配置
 */
@Configuration
@Slf4j
public class ElasticSearcConfig {

    @Value("${elasticsearch.ips}")
    String ips;
    @Value("${elasticsearch.user}")
    String user;
    @Value("${elasticsearch.password}")
    String password;
    @Value("${elasticsearch.port}")
    Integer port;
    @Value("${elasticsearch.index}")
    String index;

    @Bean(name = "restHighLevelClient")
    RestHighLevelClient restHighLevelClient(){
        log.info("初始化es客户端");
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
        String[] hosts = ips.split(",");
        HttpHost[] httpHosts = new HttpHost[hosts.length];
        for (int i = 0; i < hosts.length; i++) httpHosts[i] = new HttpHost(hosts[i], port, "http");
        RestClientBuilder builder = RestClient.builder(httpHosts)
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                        return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                }).setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
                    @Override
                    public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestBuilder) {
                        requestBuilder.setConnectTimeout(5000);
                        requestBuilder.setSocketTimeout(40000);
                        requestBuilder.setConnectionRequestTimeout(1000);
                        return requestBuilder;
                    }
                });
        log.info("es客户端初始化完成");
        return new RestHighLevelClient(builder);
    }

    public String getIndex() {
        return index;
    }
}
