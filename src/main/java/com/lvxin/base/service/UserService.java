package com.lvxin.base.service;

import com.lvxin.base.common.PageResult;
import com.lvxin.base.service.bo.UserBO;
import com.lvxin.base.web.req.QueryUserReq;

import java.util.List;

/**
 * 用户服务接口 - Service层
 */
public interface UserService {

    /**
     * 分页查询用户列表
     */
    PageResult<UserBO> getUserList(QueryUserReq req);

    /**
     * 为用户分配角色
     */
    void assignRolesToUser(Long userId, List<String> roles);

    /**
     * 获取用户角色
     */
    List<String> getUserRoles(Long userId);

    /**
     * 获取用户权限
     */
    List<String> getUserPermissions(Long userId);

    /**
     * 禁用用户
     */
    void disableUser(Long userId);

    /**
     * 启用用户
     */
    void enableUser(Long userId);

    /**
     * 发送短信验证码
     */
    void sendSmsCode(String phone);

    /**
     * 验证短信验证码
     */
    void verifySmsCode(String phone, String smsCode);

    /**
     * 用户注册
     */
    Long registerUser(String phone, String username, String password, String email);

    /**
     * 用户登录
     */
    String login(String phone, String password);

    /**
     * 用户登出
     */
    void logout(String token);

    /**
     * 强制指定用户下线
     */
    void forceUserOffline(Long userId);

    /**
     * 检查用户是否在线
     */
    boolean isUserOnline(Long userId);

    /**
     * 根据手机号查找用户ID
     *
     * @param phone 手机号
     * @return 用户ID，不存在返回null
     */
    Long findUserByPhone(String phone);

    /**
     * 验证用户密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @return 验证结果
     */
    boolean validatePassword(Long userId, String password);

    /**
     * 获取用户用户名
     *
     * @param userId 用户ID
     * @return 用户名
     */
    String getUserUsername(Long userId);

    /**
     * 获取用户手机号
     *
     * @param userId 用户ID
     * @return 手机号
     */
    String getUserPhone(Long userId);

    /**
     * 获取用户邮箱
     *
     * @param userId 用户ID
     * @return 邮箱
     */
    String getUserEmail(Long userId);

    /**
     * 获取用户昵称
     *
     * @param userId 用户ID
     * @return 昵称
     */
    String getUserNickname(Long userId);

    /**
     * 获取用户头像URL
     *
     * @param userId 用户ID
     * @return 头像URL
     */
    String getUserAvatarUrl(Long userId);

    /**
     * 检查用户账户是否激活
     *
     * @param userId 用户ID
     * @return true-账户激活，false-账户未激活
     */
    boolean isUserAccountActive(Long userId);

    /**
     * 根据ID获取用户
     */
    UserBO getUserById(Long userId);

    /**
     * 重置用户密码
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 更新当前用户资料（不允许修改手机号）
     */
    void updateUserProfile(Long userId, String nickname, String email, String gender);

    /**
     * 校验旧密码并更新为新密码
     */
    void updatePasswordWithOld(Long userId, String oldPassword, String newPassword);

    /**
     * 更新用户头像
     */
    void updateAvatar(Long userId, String avatarUrl);

    /**
     * 更新用户信息
     */
    void updateUser(Long userId, String username, String nickname, String email, String avatarUrl);

    /**
     * 删除用户及其所有相关数据
     * 警告：此操作不可逆，会删除用户的所有数据，包括订单、钱包、地址等
     * 
     * @param userId 要删除的用户ID
     * @param operatorId 操作人ID（记录谁执行了删除操作）
     * @throws BusinessException 如果用户不存在或删除失败
     */
    void deleteUserCompletely(Long userId, Long operatorId);

}