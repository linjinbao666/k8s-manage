package ama.aspect;


import ama.enumlation.CodeEnum;
import ama.exception.BizException;
import ama.util.AESUtil;
import ama.vo.UserContext;
import ama.vo.UserInfo;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 拦截器处理权限
 */

@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userHeader = request.getHeader("USER_HEADER");
        log.info("userHeader : [ {} ]", userHeader);
        Cookie[] cookies = request.getCookies();
        /**
         * 放行为空的情况，此时没有走网关
         */
        if (Strings.isEmpty(userHeader)){
            log.info("当前用户没有权限， {}", userHeader);
            return true;
//            throw new BizException(CodeEnum.ACCESS_DENIED);
        }
        String decrypt = AESUtil.decrypt(userHeader);
        UserInfo userInfo = JSON.parseObject(decrypt, UserInfo.class);
        UserContext.getUserContext().setUser(userInfo);
        UserContext.getUserContext().setCookies(cookies);
        log.info("userInfo : [ {} ]" , userInfo);
        if(null == userInfo) {
            throw new BizException(CodeEnum.NO_LOGIN);
        }
        /**
         * 放行超级管理员
         */
        if (userInfo.getUserType().equals(1)){
            return true;
        }
        /**
         * 获取namespace信息
         */
        userInfo.getDepartment();



        return true;
    }
}
