package com.lvxin.base.service;

import com.lvxin.base.service.bo.UserOnlineInfoBO;

/**
 * Token管理服务接口
 */
public interface TokenService {

    /**
     * 生成并缓存完整Token,返回代理Token
     */
    String createToken(Long userId);

    /**
     * 续期Token
     */
    void refreshToken(String proxyToken);

    /**
     * 失效Token
     */
    void removeToken(String proxyToken);

    /**
     * 获取用户在线信息
     */
    UserOnlineInfoBO getUserOnlineInfo(String proxyToken);

    /**
     * 使指定用户的所有Token失效
     */
    void invalidateUserTokens(Long userId);

    /**
     * 检查用户是否有有效的Token
     */
    boolean hasValidToken(Long userId);
}
