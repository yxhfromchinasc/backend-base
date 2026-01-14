package com.lvxin.base.service.impl;

import com.lvxin.base.constant.LoginMethod;
import com.lvxin.base.constant.LoginStatus;
import com.lvxin.base.service.TokenService;
import com.lvxin.base.service.UserService;
import com.lvxin.base.service.bo.LoginRequestBO;
import com.lvxin.base.service.bo.LoginResponseBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

/**
 * 基础登录处理器抽象类
 * 定义登录处理的通用逻辑和抽象方法
 */
@Slf4j
public abstract class BaseLoginHandler {

    @Resource
    protected TokenService tokenService;

    @Resource
    protected UserService userService;

    /**
     * 执行登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseBO login(LoginRequestBO request) {
        try {
            log.info("开始处理登录请求，登录方式: {}", getLoginMethod().getDescription());

            // 1. 验证登录参数
            validateLoginRequest(request);

            // 2. 执行具体登录逻辑
            LoginResponseBO response = doLogin(request);

            // 3. 记录登录日志
            recordLoginLog(request, response);

            // 4. 更新用户登录信息
            updateUserLoginInfo(request, response);

            log.info("登录处理完成，用户ID: {}, 结果: {}",
                    response.getUserId(), response.getSuccess() ? "成功" : "失败");

            return response;

        } catch (Exception e) {
            log.error("登录处理异常，登录方式: {}", getLoginMethod().getDescription(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    /**
     * 验证登录请求参数
     *
     * @param request 登录请求
     */
    protected abstract void validateLoginRequest(LoginRequestBO request);

    /**
     * 执行具体登录逻辑
     *
     * @param request 登录请求
     * @return 登录响应
     */
    protected abstract LoginResponseBO doLogin(LoginRequestBO request);

    /**
     * 获取登录方式
     *
     * @return 登录方式枚举
     */
    public abstract LoginMethod getLoginMethod();

    /**
     * 记录登录日志
     *
     * @param request  登录请求
     * @param response 登录响应
     */
    protected void recordLoginLog(LoginRequestBO request, LoginResponseBO response) {
        try {
            log.info("记录登录日志，用户ID: {}, 登录方式: {}, 结果: {}",
                    response.getUserId(), getLoginMethod().getDescription(),
                    response.getSuccess() ? "成功" : "失败");

            // TODO: 实现具体的日志记录逻辑
            // 可以记录到数据库或日志文件

        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    /**
     * 更新用户登录信息
     *
     * @param request  登录请求
     * @param response 登录响应
     */
    protected void updateUserLoginInfo(LoginRequestBO request, LoginResponseBO response) {
        try {
            if (response.getSuccess()) {
                log.info("更新用户登录信息，用户ID: {}", response.getUserId());

                // TODO: 实现具体的用户信息更新逻辑
                // 更新最后登录时间、登录次数等

            }
        } catch (Exception e) {
            log.error("更新用户登录信息失败", e);
        }
    }

    /**
     * 生成访问令牌
     *
     * @param userId 用户ID
     * @return 访问令牌
     */
    protected String generateAccessToken(Long userId) {
        try {
            return tokenService.createToken(userId);
        } catch (Exception e) {
            log.error("生成访问令牌失败，用户ID: {}", userId, e);
            throw new RuntimeException("生成访问令牌失败", e);
        }
    }


    /**
     * 获取令牌过期时间
     *
     * @return 过期时间戳
     */
    protected Long getTokenExpireTime() {
        try {
            // 从TokenService获取实际的token过期时间
            // 这里可以根据TokenService的配置来计算过期时间
            // 默认2小时过期（与TokenService保持一致）
            return System.currentTimeMillis() + 2 * 60 * 60 * 1000;
        } catch (Exception e) {
            log.error("获取令牌过期时间失败", e);
            // 默认2小时过期
            return System.currentTimeMillis() + 2 * 60 * 60 * 1000;
        }
    }

    // ==================== 通用用户操作方法 ====================

    /**
     * 根据手机号查找用户ID
     *
     * @param phone 手机号
     * @return 用户ID，不存在返回null
     */
    protected Long findUserByPhone(String phone) {
        try {
            return userService.findUserByPhone(phone);
        } catch (Exception e) {
            log.error("查询用户失败，手机号: {}", phone, e);
            return null;
        }
    }

    /**
     * 验证用户密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @return 验证结果
     */
    protected boolean validatePassword(Long userId, String password) {
        try {
            return userService.validatePassword(userId, password);
        } catch (Exception e) {
            log.error("验证密码失败，用户ID: {}", userId, e);
            return false;
        }
    }

    /**
     * 验证短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 验证结果
     */
    protected boolean validateVerificationCode(String phone, String code) {
        try {
            // 使用UserService的现有逻辑
            userService.verifySmsCode(phone, code);
            return true;
        } catch (Exception e) {
            log.debug("验证码验证失败，手机号: {}", phone, e);
            return false;
        }
    }

    /**
     * 创建新用户
     *
     * @param phone 手机号
     * @return 用户ID
     */
    protected Long createNewUser(String phone) {
        try {
            // 使用UserService的现有逻辑
            log.debug("创建新用户，手机号: {}", phone);
            Long userId = userService.registerUser(phone, "用户" + System.currentTimeMillis(), "123456", null);
            return userId;
        } catch (Exception e) {
            log.error("创建新用户失败，手机号: {}", phone, e);
            return null;
        }
    }

    /**
     * 获取用户用户名
     *
     * @param userId 用户ID
     * @return 用户名
     */
    protected String getUserUsername(Long userId) {
        try {
            return userService.getUserUsername(userId);
        } catch (Exception e) {
            log.error("获取用户用户名失败，用户ID: {}", userId, e);
            return "用户" + userId;
        }
    }

    /**
     * 获取用户手机号
     *
     * @param userId 用户ID
     * @return 手机号
     */
    protected String getUserPhone(Long userId) {
        try {
            return userService.getUserPhone(userId);
        } catch (Exception e) {
            log.error("获取用户手机号失败，用户ID: {}", userId, e);
            return null;
        }
    }

    /**
     * 构建成功响应
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param phone    手机号
     * @return 登录响应
     */
    protected LoginResponseBO buildSuccessResponse(Long userId, String username, String phone) {
        LoginResponseBO response = new LoginResponseBO();
        response.setSuccess(true);
        response.setLoginStatus(LoginStatus.SUCCESS.getCode());
        response.setUserId(userId);
        response.setUsername(username);
        response.setPhone(phone);
        response.setAccessToken(generateAccessToken(userId));
        response.setTokenExpireTime(getTokenExpireTime());
        response.setIsNewUser(false);
        response.setNeedBindPhone(false);
        response.setNeedCompleteInfo(false);
        return response;
    }

    /**
     * 构建新用户响应
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param phone    手机号
     * @return 登录响应
     */
    protected LoginResponseBO buildNewUserResponse(Long userId, String username, String phone) {
        LoginResponseBO response = buildSuccessResponse(userId, username, phone);
        response.setIsNewUser(true);
        response.setNeedCompleteInfo(true);
        return response;
    }


    /**
     * 构建错误响应
     *
     * @param errorMessage 错误信息
     * @return 登录响应
     */
    protected LoginResponseBO buildErrorResponse(String errorMessage) {
        LoginResponseBO response = new LoginResponseBO();
        response.setSuccess(false);
        response.setLoginStatus(LoginStatus.FAILED.getCode());
        response.setErrorMessage(errorMessage);
        return response;
    }

    /**
     * 构建需要验证码响应
     *
     * @param phone 手机号
     * @return 登录响应
     */
    protected LoginResponseBO buildNeedVerificationResponse(String phone) {
        LoginResponseBO response = new LoginResponseBO();
        response.setSuccess(false);
        response.setLoginStatus(LoginStatus.NEED_VERIFICATION.getCode());
        response.setPhone(phone);
        response.setErrorMessage("需要验证码");
        return response;
    }
}
