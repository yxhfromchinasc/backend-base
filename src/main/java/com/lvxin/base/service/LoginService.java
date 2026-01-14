package com.lvxin.base.service;

import com.lvxin.base.constant.LoginMethod;
import com.lvxin.base.service.bo.LoginRequestBO;
import com.lvxin.base.service.bo.LoginResponseBO;
import com.lvxin.base.service.impl.BaseLoginHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 登录服务
 * 统一管理各种登录方式
 */
@Service
public class LoginService {
    
    private final Map<Integer, BaseLoginHandler> handlerMap;
    
    public LoginService(List<com.lvxin.base.service.impl.BaseLoginHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                    handler -> handler.getLoginMethod().getCode(), 
                    Function.identity()
                ));
    }
    
    /**
     * 执行登录
     * @param request 登录请求
     * @return 登录响应
     */
    public LoginResponseBO login(LoginRequestBO request) {
        if (request == null) {
            throw new IllegalArgumentException("登录请求不能为空");
        }
        
        if (request.getLoginMethod() == null) {
            throw new IllegalArgumentException("登录方式不能为空");
        }
        
        BaseLoginHandler handler = getLoginHandler(request.getLoginMethod());
        return handler.login(request);
    }
    
    /**
     * 获取登录处理器
     * @param loginMethod 登录方式码
     * @return 登录处理器
     */
    public com.lvxin.base.service.impl.BaseLoginHandler getLoginHandler(Integer loginMethod) {
        if (loginMethod == null) {
            throw new IllegalArgumentException("登录方式不能为空");
        }
        
        BaseLoginHandler handler = handlerMap.get(loginMethod);
        if (handler == null) {
            throw new IllegalArgumentException("不支持的登录方式: " + loginMethod);
        }
        
        return handler;
    }
    
    /**
     * 检查登录方式是否支持
     * @param loginMethod 登录方式码
     * @return 是否支持
     */
    public boolean isLoginMethodSupported(Integer loginMethod) {
        return handlerMap.containsKey(loginMethod);
    }
    
    /**
     * 获取支持的登录方式列表
     * @return 登录方式列表
     */
    public List<LoginMethod> getSupportedLoginMethods() {
        return handlerMap.values().stream()
                .map(com.lvxin.base.service.impl.BaseLoginHandler::getLoginMethod)
                .collect(Collectors.toList());
    }
}
