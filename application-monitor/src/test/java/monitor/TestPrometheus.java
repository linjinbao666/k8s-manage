package monitor;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 测试prometheus连接类
 */
public class TestPrometheus {

    public static void main(String[] args) throws URISyntaxException {

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> exchange = restTemplate.exchange(new URI("http://*.16.12.21:30003/api/v1/targets?state=active"),
                HttpMethod.GET, null, String.class);
        System.out.println(exchange);
    }

}
