package monitor.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import monitor.dao.AlertHistoryDao;
import monitor.dao.AlertRuleDao;
import monitor.entity.AlertHistory;
import monitor.entity.AlertRule;
import monitor.service.AlertRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.StringReader;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/webhook")
public class WebHookController {

    @Autowired
    AlertRuleDao alertRuleDao;

    @Autowired
    AlertHistoryDao alertHistoryDao;

    @RequestMapping("/")
    public String webhook(@RequestBody String body) {
        JSONObject jsonObject = JSON.parseObject(body);
        JSONArray alerts = jsonObject.getJSONArray("alerts");
        JSONObject commonLabels = jsonObject.getJSONObject("commonLabels");
        String alertname = commonLabels.getString("alertname");
        log.info("收到webhook告警alertname:{}", alertname);
        AlertRule byName = alertRuleDao.findByEnName(alertname);
        if (null == byName) {
            log.info("数据库不存在的告警规则");
            return "success";
        }
        AlertHistory alertHistory = new AlertHistory();
        alertHistory.setContainerName(byName.getAppName());
        alertHistory.setMail(1);
        alertHistory.setQuota(byName.getQuota());
        alertHistory.setRuleId(byName.getId());
        alertHistory.setTarget(byName.getTarget());
        alertHistoryDao.save(alertHistory);
        boolean isLogin = login();
        if (!isLogin){
            log.info("登录失败，发送邮件失败！");
            return "error";
        }
        boolean b = sendMail(alertname, "application-monitor",
                byName.getUser(),"系统达到告警值");
        if (!b) {
            log.info("发送失败");
        }
        return "success";
    }

    /**
     * 发送告警邮件
     * @param name
     * @param businessType
     * @param toUserId
     * @param memo
     */
    boolean sendMail(String name, String businessType,String toUserId,String memo){
        String alertUrl = LOGIN_URL+"/workcenter/api/v1/alarminfo/create";
        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("name", name);
        map.add("businessType", businessType);
        map.add("toUserId", toUserId);
        map.add("memo", memo);
        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<MultiValueMap<String, String>>(map,null);
        HttpEntity<String> response = restTemplate.exchange(alertUrl,
                HttpMethod.POST, requestEntity, String.class);
        HttpHeaders responseHeader = response.getHeaders();
        String body = response.getBody();
        JSONObject jsonObject = JSONObject.parseObject(body);
        int code = (Integer) jsonObject.get("code");
        log.info("登录状态码，code = " + code);
        if (code != 200){
            log.info("登录失败 ");
            return false;
        }
        return true;
    }

    @Autowired
    RestTemplate restTemplate;
    @Value("${approval.login_url}")
    String LOGIN_URL;
    @Value("${approval.approval_url}")
    String APPROVAL_URL;
    @Value("${approval.username}")
    String USERNAME;
    @Value("${approval.md5Password}")
    String MD5PASSWORD;

    private static HttpHeaders headers = null;
    public boolean login(){
        boolean b = false;
        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("username", USERNAME);
        map.add("md5Password", MD5PASSWORD);
        HttpEntity<MultiValueMap<String, String>> requestEntity =
                new HttpEntity<MultiValueMap<String, String>>(map,null);
        HttpEntity<String> response = restTemplate.exchange(LOGIN_URL+"/workcenter/api/v1/user/loginWithNoVCode",
                HttpMethod.POST, requestEntity, String.class);
        HttpHeaders responseHeader = response.getHeaders();
        String body = response.getBody();

        JSONObject jsonObject = JSONObject.parseObject(body);
        int code = (Integer) jsonObject.get("code");
        log.info("登录状态码，code = " + code);
        if (code != 200){
            log.info("登录失败 ");
            return false;
        }
        log.info("登录获取到的header为：" + responseHeader);
        log.info("登录成功，开始构建请求Cookie");
        headers = new HttpHeaders();
        List<String> list = responseHeader.get("Set-Cookie");
        for (int i=0; i< list.size(); i++) {
            headers.add("Cookie", list.get(i).split(";")[0]);
        }

        return true;
    }
}
