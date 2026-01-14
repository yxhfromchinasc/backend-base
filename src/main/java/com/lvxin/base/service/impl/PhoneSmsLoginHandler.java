package com.lvxin.base.service.impl;

import com.lvxin.base.constant.LoginMethod;
import com.lvxin.base.service.bo.LoginRequestBO;
import com.lvxin.base.service.bo.LoginResponseBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 手机号验证码登录处理器
 * 处理手机号+验证码的登录方式
 */
@Slf4j
@Service
public class PhoneSmsLoginHandler extends BaseLoginHandler {

    @Override
    protected void validateLoginRequest(LoginRequestBO request) {
        if (request == null) {
            throw new IllegalArgumentException("登录请求不能为空");
        }

        if (!StringUtils.hasText(request.getPhone())) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        if (!StringUtils.hasText(request.getVerificationCode())) {
            throw new IllegalArgumentException("验证码不能为空");
        }

        // 验证手机号格式
        if (!isValidPhone(request.getPhone())) {
            throw new IllegalArgumentException("手机号格式不正确");
        }

        // 验证验证码格式
        if (!isValidVerificationCode(request.getVerificationCode())) {
            throw new IllegalArgumentException("验证码格式不正确");
        }

        log.debug("手机号验证码登录参数验证通过，手机号: {}", request.getPhone());
    }

    @Override
    protected LoginResponseBO doLogin(LoginRequestBO request) {
        try {
            log.info("开始手机号验证码登录，手机号: {}", request.getPhone());

            // 1. 验证验证码
            if (!validateVerificationCode(request.getPhone(), request.getVerificationCode())) {
                log.warn("验证码错误，手机号: {}", request.getPhone());
                return buildErrorResponse("验证码错误");
            }

            // 2. 根据手机号查询用户
            Long userId = findUserByPhone(request.getPhone());

            // 3. 如果用户不存在，创建新用户
            boolean isNewUser = false;
            if (userId == null) {
                userId = createNewUser(request.getPhone());
                if (userId == null) {
                    log.error("创建新用户失败，手机号: {}", request.getPhone());
                    return buildErrorResponse("创建用户失败");
                }
                log.info("创建新用户成功，用户ID: {}, 手机号: {}", userId, request.getPhone());
                isNewUser = true;
            }

            // 4. 检查账户状态
            if (!userService.isUserAccountActive(userId)) {
                log.warn("账户被锁定，用户ID: {}", userId);
                return buildErrorResponse("账户被锁定");
            }

            // 5. 获取用户信息
            String username = getUserUsername(userId);

            // 6. 构建响应
            LoginResponseBO response;
            if (isNewUser) {
                response = buildNewUserResponse(userId, username, request.getPhone());
            } else {
                response = buildSuccessResponse(userId, username, request.getPhone());
            }

            // 7. 清除验证码
            clearVerificationCode(request.getPhone());

            log.info("手机号验证码登录成功，用户ID: {}", userId);
            return response;

        } catch (Exception e) {
            log.error("手机号验证码登录异常，手机号: {}", request.getPhone(), e);
            return buildErrorResponse("登录失败: " + e.getMessage());
        }
    }

    @Override
    public LoginMethod getLoginMethod() {
        return LoginMethod.PHONE_SMS;
    }

    /**
     * 验证手机号格式
     *
     * @param phone 手机号
     * @return 是否有效
     */
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 验证验证码格式
     *
     * @param code 验证码
     * @return 是否有效
     */
    private boolean isValidVerificationCode(String code) {
        return code != null && code.matches("^\\d{6}$");
    }

    /**
     * 清除验证码
     *
     * @param phone 手机号
     */
    private void clearVerificationCode(String phone) {
        try {
            // 验证码在验证成功后已经自动清除，这里不需要额外操作
            // UserService.verifySmsCode() 方法中已经调用了 redisService.delete(redisKey)
            log.debug("验证码已自动清除，手机号: {}", phone);
        } catch (Exception e) {
            log.error("清除验证码失败，手机号: {}", phone, e);
        }
    }
}
