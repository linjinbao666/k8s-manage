package ecs.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ecs.service.ApprovalService;
import ecs.vo.Approval;
import ecs.vo.CodeEnum;
import ecs.vo.ResultVo;
import ecs.vo.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;

@Service
@Slf4j
public class ApprovalServiceImpl implements ApprovalService {
    @Autowired
    private RestTemplate restTemplate;
    @Value("${approval.gateway}")
    private String gateway;

    @Override
    public void execute(Approval approval) {
        String state = approval.getState();
        approval.getApprovalKey();
        approval.getObjectMemo();
        approval.getApplicantUserId();
    }

    @Override
    public ResultVo send2Approval(Approval approval) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("businessType", approval.getBusinessType());
        map.add("objectName", approval.getObjectName());
        map.add("objectMemo", approval.getObjectMemo());
        map.add("detailMessage", approval.getDetailMessage());
        map.add("toApprovalUserId", null);
        UserContext userContext = UserContext.getUserContext();
        MultiValueMap<String, String> headers = new HttpHeaders();
        for (int i = 0; i < userContext.getCookies().length; i++) {
            Cookie cookie = userContext.getCookies()[i];
            headers.add("Cookie", cookie.getName() + "=" + cookie.getValue());
        }
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        ResponseEntity<String> exchange = restTemplate.exchange(gateway + "/workcenter/api/v1/approval/create",
                HttpMethod.POST, requestEntity, String.class);
        int status = exchange.getStatusCodeValue();
        if (status != 200) {
            return ResultVo.renderErr(CodeEnum.ERR).withRemark("发送审核失败！");
        }
        Gson gson = new Gson();
        String body = exchange.getBody();
        JsonObject data = gson.fromJson(body, JsonObject.class);
        return ResultVo.renderOk(data.toString()).withRemark("发送审批成功");
    }

    @Override
    public void sendResult(String approvalKey, String message, String success) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("approvalKey", approvalKey);
        map.add("message", message);
        map.add("success", success);
        UserContext userContext = UserContext.getUserContext();
        MultiValueMap<String, String> headers = new HttpHeaders();
        for (int i = 0; i < userContext.getCookies().length; i++) {
            Cookie cookie = userContext.getCookies()[i];
            headers.add("Cookie", cookie.getName() + "=" + cookie.getValue());
        }
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        ResponseEntity<String> exchange = restTemplate.exchange(gateway + "/workcenter/api/v1/approval/setexecutedresult",
                HttpMethod.POST, requestEntity, String.class);
        log.info("发送执行结果,{}", exchange.getBody());
    }

}
