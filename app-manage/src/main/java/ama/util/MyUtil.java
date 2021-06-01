package ama.util;

import ama.enumlation.UnzipEnum;
import ama.vo.UserContext;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.apache.directory.api.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String工具类
 */
@Slf4j
public class MyUtil {
    /**
     * 判断String你能否转成数字
     * @param str 字符串
     * @return
     */
    public static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if(!isNum.matches() ){
            return false;
        }
        return true;
    }

    /**
     * 校验是否合法的base64格式
     * @param str
     * @return
     */
    public static boolean isBase64(String str) {
        String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
        return Pattern.matches(base64Pattern, str);
    }

    /**
     * 解压文件
     * @param src 源文件压缩包
     * @param dest 目标文件文件夹
     * @return 返回最终文件存在的未知
     */
    public static EnumMap<UnzipEnum, String> unzip(String src, String dest) throws ZipException {
        EnumMap resultMap =  new EnumMap<UnzipEnum, String>(UnzipEnum.class);
        if (!new File(dest).exists() || !new File(dest).isDirectory()) {
            resultMap.put(UnzipEnum.ERROR,"目标文件夹不存在或者目标地址不是文件夹");
            return resultMap;
        }
        ZipFile zipFile = new ZipFile(src);
        List<FileHeader> fileHeaders = zipFile.getFileHeaders();
        if (fileHeaders.size() < 1) {
            resultMap.put(UnzipEnum.ERROR,"压缩包为空，请检查");
            return resultMap;
        }
        FileHeader dockerfile = fileHeaders.stream()
                .filter(fileHeader -> fileHeader.getFileName().contains("Dockerfile"))
                .findAny().orElse(null);
        if (null == dockerfile){
            resultMap.put(UnzipEnum.ERROR,"Dockerfile文件不存在");
            return resultMap;
        }
        FileHeader isFolderZip = fileHeaders.stream()
                .filter(fileHeader -> fileHeader.isDirectory()).findAny()
                .orElse(null);
        zipFile.extractAll(dest);

        if (null != isFolderZip){
            resultMap.put(UnzipEnum.UNZIP_FOLDER,dest+"/"+isFolderZip.getFileName());
            resultMap.put(UnzipEnum.DOCKERFILE,dest+"/"+dockerfile.getFileName());
        }else {
            resultMap.put(UnzipEnum.UNZIP_FOLDER,dest);
            resultMap.put(UnzipEnum.DOCKERFILE,dest+"/"+dockerfile.getFileName());
        }
        resultMap.put(UnzipEnum.OK,"解压成功");

        log.info("resultMap = "+ resultMap);
        return resultMap;
    }

    /**
     * 加密
     * @param str
     * @return
     */
    public static String JdkBase64(String str){
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(str.getBytes());
    }

    /**
     * 解密
     * @param str
     * @return
     */
    public static String deCodeBase64(String str){
        Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(str));
    }

    /**
     * 获取名称空间
     * @param restTemplate
     * @param gateway
     * @return
     */
    public static String namespace(RestTemplate restTemplate, String gateway){
        String namespace = null;
        UserContext userContext = UserContext.getUserContext();
        HttpHeaders headers = new HttpHeaders();
        for (int i=0; i< userContext.getCookies().length; i++) {
            Cookie cookie = userContext.getCookies()[i];
            headers.add("Cookie", cookie.getName() + "=" + cookie.getValue());
        }
        HttpEntity request = new HttpEntity(headers);
        ResponseEntity<String> exchange = restTemplate.exchange(gateway +
                "/resource-manage/resourceRequest/namespace"
                + "?regId=" + userContext.getUser().getUnit(),
                HttpMethod.GET, request, String.class);
        if (exchange.getStatusCode().equals(HttpStatus.OK)){
            JSONObject jsonObject = JSONObject.parseObject(exchange.getBody());
            JSONArray data = (JSONArray) jsonObject.get("data");
            if (data!=null && data.size()>0) {
                namespace = (String) data.getJSONObject(0).get("namespace");
            }
        }
        return namespace;
    }

    /**
     * 通过ip获取信息,loss:丢包率，delay:延时
     * @return
     */
    private static Map<String,String> ns(String ip){
        String os = System.getProperty("os.name");
        Map<String,String> networkMap = new HashMap<>(3);
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        String line = null;
        InputStream inputStream = null;
        InputStreamReader isReader = null;
        BufferedReader reader = null;

        String cmd = "ping "+ ip;
        log.info("操作系统：" + os);

        String loss = "";       //丢包率
        String delay = "";      //延时
        try {
            process = runtime.exec(cmd);
            inputStream = process.getInputStream();//实例化输入流
            isReader = new InputStreamReader(inputStream,"GB2312");
            reader = new BufferedReader(isReader);
            StringBuffer buffer = new StringBuffer();
            if (os.contains("win") || os.contains("Win")|| os.contains("WIN")){
                while ((line = reader.readLine()) != null){
                    //丢包率
                    if (line.contains("%")){
                        loss = line.substring(line.lastIndexOf("=") + 1 ,line.indexOf("%") + 1);
                        if (loss.contains("(")){
                            loss = loss.substring(loss.indexOf("(") + 1).trim();
                        }
                        System.out.println(loss);
                    }
                    //网络延时
                    if ((line.contains(",") || line.contains("，")) && line.contains("=") && line.contains("ms")){
                        delay = line.substring(line.lastIndexOf("=") + 1 ,line.lastIndexOf("ms") + 2).trim();
                        log.info(delay);
                    }
                    buffer.append(line + "\n");
                }
            }else{
                while ((line = reader.readLine()) != null){
                    //丢包率
                    if (line.contains("%")){
                        String[] msg = null;
                        if (line.contains(",")){
                            msg = line.split(",");
                        }else if (line.contains("，")){
                            msg = line.split("，");
                        }
                        if (msg.length > 0) {
                            loss = msg[2].substring(0, msg[2].indexOf("%") + 1).trim();
                            log.info(loss);
                        }
                    }
                    //网络延时
                    if (line.contains("/")){
                        String[] msg = line.split("=");
                        String[] names = msg[0].split("/");
                        String[] values = msg[1].split("/");
                        for (int i = 0;i < names.length;i++){
                            String str = names[i];
                            if ("avg".equalsIgnoreCase(str)){
                                delay = values[i];
                                break;
                            }
                        }
                        log.info(delay);
                    }
                    buffer.append(line + "\n");
                }
            }

            if (Strings.isNotEmpty(loss)){
                networkMap.put("loss",loss);
            }else {
                networkMap.put("loss","0%");
            }
            if (Strings.isNotEmpty(delay)){
                networkMap.put("delay",delay);
            }else {
                networkMap.put("delay","99999ms");
                networkMap.put("status","-1");
            }
            log.info(buffer.toString());
            inputStream.close();
            isReader.close();
            reader.close();
        } catch (IOException e) {
            log.error("通过ping方式获取网络信息异常：" + e.getMessage());
            e.printStackTrace();
        }
        return networkMap;
    }

}
