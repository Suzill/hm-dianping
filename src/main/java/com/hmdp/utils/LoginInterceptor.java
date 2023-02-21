package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断是否需要拦截（用户是否存在）
        UserDTO user = UserHolder.getUser();
        if (user == null){
            // 不存在拦截，返回状态码401
            response.setStatus(401);
            return false;
        }
        // 放行
        return true;
    }
}
