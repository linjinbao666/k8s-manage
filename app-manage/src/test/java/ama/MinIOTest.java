package ama;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.http.*;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;

public class MinIOTest {

    public static void main(String[] args) throws Exception {
        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://*.16.12.22:9000")
                .credentials("minioadmin", "minioadmin")
                .build();

//        minioClient.putObject(PutObjectArgs.builder().build());
//        InputStream object = minioClient.getObject(GetObjectArgs.builder().bucket("fline").object("ll_develop.sql").build());
//        byte[] bytes = new byte[1024];
//        int index = 0;
//        FileOutputStream fileOutputStream = new FileOutputStream ("a");
//        while ((index = object.read(bytes)) !=-1){
//            fileOutputStream.write(bytes, 0, index);
//            fileOutputStream.flush();
//        }
//        fileOutputStream.close();
//        object.close();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        RequestCallback requestCallback = request -> request.getHeaders()
                .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        String url = "http://*.16.12.22:9000/fline/%E5%AD%90%E7%B3%BB%E7%BB%9F%E5%88%92%E5%88%86.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20200906%2F%2Fs3%2Faws4_request&X-Amz-Date=20200906T140357Z&X-Amz-Expires=432000&X-Amz-SignedHeaders=host&X-Amz-Signature=cca1da0309ce4fe3ee377ce56d1e189f262c7946ff0237e6f3bbf3edcc4b6316";
        ResponseEntity<byte[]> entity =
                restTemplate.exchange(url,HttpMethod.GET,new HttpEntity<>(headers), byte[].class,requestCallback);
        byte[] body = entity.getBody();
        InputStream sbs = new ByteArrayInputStream(body);

        byte[] bytes = new byte[1024];
        int index = 0;
        FileOutputStream fileOutputStream = new FileOutputStream ("b");
        while ((index = sbs.read(bytes)) !=-1){
            fileOutputStream.write(bytes, 0, index);
            fileOutputStream.flush();
        }
        fileOutputStream.close();
        sbs.close();
    }
}
