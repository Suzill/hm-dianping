package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getCode(String phone, HttpSession session) {
        // 验证手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 不符合
            return Result.fail("手机号格式错误");
        }
        // 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
//         保存验证码到session
//         session.setAttribute("code", code);
//         保存验证码到redis
        String loginKey = LOGIN_CODE_KEY + phone;
        stringRedisTemplate.opsForValue().set(loginKey, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 发送验证码
        log.info("发送验证码成功: {}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 验证手机号和验证码是否正确
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            // 不符合
            return Result.fail("手机号格式错误");
        }
//        Object code = session.getAttribute("code");
        String loginKey = LOGIN_CODE_KEY + loginForm.getPhone();
        String code = stringRedisTemplate.opsForValue().get(loginKey);
        if (!loginForm.getCode().equals(code)){
            return Result.fail("验证码错误");
        }
        // 根据手机号查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, loginForm.getPhone());
        User user = this.getOne(queryWrapper);
        // 用户不存在（新建用户保存数据库，存入session）
        if (user == null){
            user = createUserWithPhone(loginForm.getPhone());
        }
//         用户存在（存入session）
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
//        用户存在，存入redis
        // 1.随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString();
        // 2.将User转化为map对象
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object>  userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(), 
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((filedName, filedValue) -> filedValue.toString()));
        // 3.存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, CACHE_NULL_TTL, TimeUnit.HOURS);
        // 返回token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setPassword("000000");
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(7));
        this.save(user);
        return user;
    }
}
