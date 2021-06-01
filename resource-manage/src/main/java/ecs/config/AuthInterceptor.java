package ecs.config;


import com.google.gson.Gson;
import ecs.dao.ResourceRequestDao;
import ecs.exception.BizException;
import ecs.util.AESUtil;
import ecs.vo.CodeEnum;
import ecs.vo.UserContext;
import ecs.vo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    ResourceRequestDao resourceRequestDao;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userHeader = request.getHeader("USER_HEADER");
        log.info("userHeader : [ {} ]", userHeader);
        Cookie[] cookies = request.getCookies();
        if (Strings.isEmpty(userHeader)){
            log.info("当前用户没有权限. {}", userHeader);
            throw new BizException(CodeEnum.ACCESS_DENIED);
        }
        Gson gson = new Gson();
        String decrypt = AESUtil.decrypt(userHeader);
        UserInfo userInfo = gson.fromJson(decrypt, UserInfo.class);
        UserContext.getUserContext().setUser(userInfo);
        UserContext.getUserContext().setCookies(cookies);
        log.info("userInfo : [ {} ]" , userInfo);
        if(null == userInfo) {
            throw new BizException(CodeEnum.NO_LOGIN);
        }

        return true;
    }
}
