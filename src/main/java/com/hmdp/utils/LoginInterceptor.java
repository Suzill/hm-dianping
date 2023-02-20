package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        // 1.获取session
//        HttpSession session = request.getSession();
//        // 2.获取session中的用户
//        Object user = session.getAttribute("user");
//        // 3.判断用户是否存在
//        if (user == null){
//            // 4.不存在（拦截）, 返回401状态码
//            response.setStatus(401);
//            return false;
//        }
//        // 5.存在（保存用户信息到ThreadLocal中
//        UserHolder.saveUser((UserDTO) user);
//        // 6.放行
        // 1.获取请求头中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)){
            // 不存在（拦截）, 返回401状态码
            response.setStatus(401);
            return false;
        }
        // 2.基于token获取redis中的用户
        String tokenKey = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = redisTemplate.opsForHash().entries(tokenKey);
        // 3.判断用户是否存在
        if (userMap.isEmpty()){
            // 不存在（拦截）, 返回401状态码
            response.setStatus(401);
            return false;
        }
        // 4.将查询到的hash数据转为userDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 5.存在（保存用户信息到ThreadLocal中
        UserHolder.saveUser(userDTO);
        // 6.刷新token有效期
        redisTemplate.expire(tokenKey, CACHE_NULL_TTL, TimeUnit.HOURS);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
