package com.lvxin.base.service.impl;

import com.lvxin.base.constant.LoginMethod;
import com.lvxin.base.service.bo.LoginRequestBO;
import com.lvxin.base.service.bo.LoginResponseBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 手机号密码登录处理器
 * 处理手机号+密码的登录方式
 */
@Slf4j
@Service
public class PhonePasswordLoginHandler extends BaseLoginHandler {

    @Override
    protected void validateLoginRequest(LoginRequestBO request) {
        if (request == null) {
            throw new IllegalArgumentException("登录请求不能为空");
        }

        if (!StringUtils.hasText(request.getPhone())) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 验证手机号格式
        if (!isValidPhone(request.getPhone())) {
            throw new IllegalArgumentException("手机号格式不正确");
        }

        log.debug("手机号密码登录参数验证通过，手机号: {}", request.getPhone());
    }

    @Override
    protected LoginResponseBO doLogin(LoginRequestBO request) {
        try {
            log.info("开始手机号密码登录，手机号: {}", request.getPhone());

            // 1. 根据手机号查询用户
            Long userId = findUserByPhone(request.getPhone());
            if (userId == null) {
                log.warn("用户不存在，手机号: {}", request.getPhone());
                return buildErrorResponse("用户不存在");
            }

            // 2. 验证密码
            if (!validatePassword(userId, request.getPassword())) {
                log.warn("密码错误，用户ID: {}", userId);
                return buildErrorResponse("密码错误");
            }

            // 3. 检查账户状态
            if (!userService.isUserAccountActive(userId)) {
                log.warn("账户被锁定，用户ID: {}", userId);
                return buildErrorResponse("账户被锁定");
            }

            // 4. 获取用户信息
            String username = getUserUsername(userId);

            // 5. 构建成功响应
            LoginResponseBO response = buildSuccessResponse(userId, username, request.getPhone());
            log.info("手机号密码登录成功，用户ID: {}", userId);

            return response;

        } catch (Exception e) {
            log.error("手机号密码登录异常，手机号: {}", request.getPhone(), e);
            return buildErrorResponse("登录失败: " + e.getMessage());
        }
    }

    @Override
    public LoginMethod getLoginMethod() {
        return LoginMethod.PHONE_PASSWORD;
    }

    /**
     * 验证手机号格式
     *
     * @param phone 手机号
     * @return 是否有效
     */
    private boolean isValidPhone(String phone) {
        // TODO: 实现手机号格式验证
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }


}
