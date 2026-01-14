package com.lvxin.base.service.impl;

import com.lvxin.base.constant.UserStatus;
import com.lvxin.base.common.PageResult;
import com.lvxin.base.repository.UserRepository;
import com.lvxin.base.repository.RoleRepository;





import com.lvxin.base.repository.entity.UserDO;
import com.lvxin.base.repository.entity.RoleDO;
import com.lvxin.base.service.UserService;
import com.lvxin.base.service.TokenService;
import com.lvxin.base.service.bo.UserBO;
import com.lvxin.base.web.converter.UserConverter;
import com.lvxin.base.web.req.QueryUserReq;
import com.lvxin.base.util.JsonUtil;
import com.lvxin.base.util.PasswordUtil;
import com.lvxin.base.service.RedisService;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /**
     * 短信验证码Redis键前缀
     */
    private static final String SMS_CODE_REDIS_PREFIX = "sms_code:";

    /**
     * 短信验证码过期时间（分钟）
     */
    private static final int SMS_CODE_EXPIRE_MINUTES = 5;

    /**
     * 短信验证码长度
     */
    private static final int SMS_CODE_LENGTH = 6;

    @Resource
    private UserRepository userRepository;

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private TokenService tokenService;

    @Resource
    private RedisService redisService;

    

    

    

    

    

    

    

    

    

    

    

    


    

    

    

    

    

    

    @Override
    public PageResult<UserBO> getUserList(QueryUserReq req) {
        log.info("查询用户列表，页码: {}, 页大小: {}, 用户名: {}, 手机号: {}, 邮箱: {}, 状态: {}",
                req.getPageNum(), req.getPageSize(), req.getUsername(), req.getPhone(), req.getEmail(), req.getStatus());

        // 构建查询条件
        QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();

        if (req.getUsername() != null && !req.getUsername().trim().isEmpty()) {
            queryWrapper.like("username", req.getUsername().trim());
        }
        if (req.getPhone() != null && !req.getPhone().trim().isEmpty()) {
            queryWrapper.like("phone", req.getPhone().trim());
        }
        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            queryWrapper.like("email", req.getEmail().trim());
        }
        if (req.getStatus() != null) {
            queryWrapper.eq("status", req.getStatus());
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc("created_at");

        // 分页查询
        Page<UserDO> page = new Page<>(req.getPageNumInt(), req.getPageSizeInt());
        Page<UserDO> result = userRepository.selectPage(page, queryWrapper);

        // 转换为BO对象
        List<UserBO> userBOList = result.getRecords().stream()
                .map(this::convertToBO)
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<UserBO> pageResult = new PageResult<>();
        pageResult.setList(userBOList);
        pageResult.setTotal(result.getTotal());
        pageResult.setPage(req.getPageNum());
        pageResult.setPageSize(req.getPageSize());

        return pageResult;
    }

    @Override
    public void assignRolesToUser(Long userId, List<String> roles) {
        log.info("为用户 {} 分配角色: {}", userId, roles);

        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            throw new RuntimeException("用户不存在");
        }

        try {
            String rolesJson = JsonUtil.toJson(roles);
            userDO.setRoles(rolesJson);
            userRepository.updateById(userDO);

            // 角色变更后，使用户所有Token失效
//            tokenService.invalidateUserTokens(userId);
            log.info("用户 {} 角色变更，已使所有Token失效", userId);

        } catch (Exception e) {
            log.error("分配角色失败", e);
            throw new RuntimeException("分配角色失败");
        }
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        UserDO userDO = userRepository.selectById(userId);
        String rolesJson = userDO == null ? null : userDO.getRoles();
        return rolesJson == null ? List.of() : JsonUtil.toList(rolesJson, String.class);
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        List<String> userRoles = getUserRoles(userId);
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }

        // 批量查询所有角色，减少 DB 往返
        List<RoleDO> roles = roleRepository.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RoleDO>()
                        .in("role_code", userRoles)
        );

        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        // 使用 Stream + JsonUtil 一步到位解析与扁平化，并去重
        return roles.stream()
                .filter(Objects::nonNull)
                .map(RoleDO::getPermissions)
                .filter(Objects::nonNull)
                .flatMap(json -> JsonUtil.toList(json, String.class).stream())
                .distinct()
                .collect(Collectors.toList());
    }


    @Override
    public void disableUser(Long userId) {
        log.info("禁用用户: {}", userId);

        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            throw new RuntimeException("用户不存在");
        }

        userDO.setStatus(UserStatus.DISABLED.getCode());
        userRepository.updateById(userDO);

        log.info("用户 {} 已禁用", userId);
    }

    @Override
    public void enableUser(Long userId) {
        log.info("启用用户: {}", userId);

        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            throw new RuntimeException("用户不存在");
        }

        userDO.setStatus(UserStatus.NORMAL.getCode());
        userRepository.updateById(userDO);

        log.info("用户 {} 已启用", userId);
    }

    @Override
    public void sendSmsCode(String phone) {
        log.info("发送短信验证码到手机号: {}", phone);
        String redisKey = SMS_CODE_REDIS_PREFIX + phone;

        if (redisService.exists(redisKey)) {
            throw new BusinessException(ErrorCode.SMS_CODE_FREQUENTLY);
        }

        // 生成6位数字验证码
        String smsCode = generateSmsCode();

        // 将验证码存储到Redis，设置过期时间
        redisService.set(redisKey, smsCode, Duration.ofMinutes(SMS_CODE_EXPIRE_MINUTES));

        // 模拟发送短信（实际项目中应该调用短信服务商API）
        log.info("模拟发送短信验证码 {} 到手机号 {}", smsCode, phone);

        // TODO: 实际项目中应该调用短信服务商API
        // smsService.sendSms(phone, smsCode);
    }

    @Override
    public void verifySmsCode(String phone, String smsCode) {
        log.info("验证手机号 {} 的验证码", phone);

        String redisKey = SMS_CODE_REDIS_PREFIX + phone;
        String storedCode = redisService.get(redisKey, String.class);

        if (storedCode == null) {
            throw new BusinessException(ErrorCode.SMS_CODE_EXPIRED);
        }

        if (!storedCode.equals(smsCode)) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }

        // 验证成功后删除验证码
        redisService.delete(redisKey);
    }

    @Override
    public Long registerUser(String phone, String username, String password, String email) {

        // 创建新用户
        UserDO userDO = new UserDO();
        userDO.setPhone(phone);
        userDO.setUsername(username);
        userDO.setPassword(PasswordUtil.encode(password));
        userDO.setEmail(email);
        userDO.setNickname(username); // 默认昵称为用户名
        userDO.setStatus(UserStatus.NORMAL.getCode()); // 1表示正常状态
        userDO.setRoles(JsonUtil.toJson(java.util.List.of("USER"))); // 默认赋予普通用户角色

        userRepository.insert(userDO);

        Long userId = userDO.getId();
        log.info("用户注册成功，用户ID: {}", userId);

        return userId;
    }

    @Override
    public UserBO getUserById(Long userId) {
        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) return null;
        return UserConverter.doToBo(userDO);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }
        userDO.setPassword(PasswordUtil.encode(newPassword));
        userRepository.updateById(userDO);
    }

    @Override
    public void updateUserProfile(Long userId, String nickname, String email, String gender) {
        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }
        // 不允许修改手机号
        if (nickname != null) userDO.setNickname(nickname);
        if (email != null) userDO.setEmail(email);
        // 性别字段若DO未定义可忽略；此处不持久化gender
        userRepository.updateById(userDO);
    }

    @Override
    public void updatePasswordWithOld(Long userId, String oldPassword, String newPassword) {
        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }
        if (!PasswordUtil.matches(oldPassword, userDO.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        userDO.setPassword(PasswordUtil.encode(newPassword));
        userRepository.updateById(userDO);
    }

    @Override
    public void updateAvatar(Long userId, String avatarUrl) {
        log.info("开始更新用户头像，用户ID: {}, 头像URL: {}", userId, avatarUrl);
        
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            log.warn("头像URL为空，用户ID: {}", userId);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像URL不能为空");
        }
        
        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            log.error("用户不存在，用户ID: {}", userId);
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }
        
        log.info("更新前头像URL: {}", userDO.getAvatarUrl());
        userDO.setAvatarUrl(avatarUrl.trim());
        
        // 使用 UpdateWrapper 确保更新 avatar_url 字段
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<UserDO> updateWrapper = 
            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        updateWrapper.eq("id", userId)
                    .set("avatar_url", avatarUrl.trim());
        
        int updateCount = userRepository.update(updateWrapper);
        log.info("更新头像结果，影响行数: {}, 用户ID: {}, 新头像URL: {}", updateCount, userId, avatarUrl);
        
        if (updateCount <= 0) {
            log.error("更新头像失败，影响行数为0，用户ID: {}", userId);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新头像失败");
        }
    }

    @Override
    public void updateUser(Long userId, String username, String nickname, String email, String avatarUrl) {
        log.info("开始更新用户信息，用户ID: {}", userId);
        
        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            log.error("用户不存在，用户ID: {}", userId);
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }
        
        // 更新字段（只更新非空值）
        if (username != null && !username.trim().isEmpty()) {
            // 检查用户名是否已被其他用户使用
            QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", username.trim()).ne("id", userId);
            UserDO existingUser = userRepository.selectOne(queryWrapper);
            if (existingUser != null) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已被使用");
            }
            userDO.setUsername(username.trim());
        }
        
        if (nickname != null) {
            userDO.setNickname(nickname.trim());
        }
        
        if (email != null) {
            userDO.setEmail(email.trim());
        }
        
        if (avatarUrl != null) {
            userDO.setAvatarUrl(avatarUrl.trim());
        }
        
        userRepository.updateById(userDO);
        log.info("用户信息更新成功，用户ID: {}", userId);
    }

    @Override
    public String login(String phone, String password) {
        log.info("用户登录: phone={}", phone);

        // 查询用户信息
        QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        UserDO userDO = userRepository.selectOne(queryWrapper);

        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        // 检查用户状态
        if (userDO.getStatus() != UserStatus.NORMAL.getCode()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 验证密码
        if (!PasswordUtil.matches(password, userDO.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 生成Token
        String token = tokenService.createToken(userDO.getId());

        log.info("用户登录成功: phone={}", phone);
        return token;
    }

    @Override
    public void logout(String token) {
        log.info("用户登出: token={}", token);
        tokenService.removeToken(token);
    }

    @Override
    public void forceUserOffline(Long userId) {
        log.info("强制用户下线: userId={}", userId);

        // 检查用户是否存在
        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        // 使该用户的Token失效
        tokenService.invalidateUserTokens(userId);

        log.info("用户 {} 强制下线完成", userId);
    }

    @Override
    public boolean isUserOnline(Long userId) {
        log.debug("检查用户是否在线: userId={}", userId);

        // 检查用户是否存在
        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            return false;
        }

        // 检查用户是否有有效Token
        return tokenService.hasValidToken(userId);
    }

    @Override
    public Long findUserByPhone(String phone) {
        try {
            log.debug("根据手机号查询用户: {}", phone);

            QueryWrapper<UserDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("phone", phone);
            UserDO userDO = userRepository.selectOne(queryWrapper);

            if (userDO == null) {
                log.debug("用户不存在，手机号: {}", phone);
                return null;
            }

            log.debug("找到用户，手机号: {}, 用户ID: {}", phone, userDO.getId());
            return userDO.getId();

        } catch (Exception e) {
            log.error("根据手机号查询用户失败，手机号: {}", phone, e);
            return null;
        }
    }

    @Override
    public boolean validatePassword(Long userId, String password) {
        try {
            log.debug("验证用户密码，用户ID: {}", userId);

            UserDO userDO = userRepository.selectById(userId);
            if (userDO == null) {
                log.warn("用户不存在，用户ID: {}", userId);
                return false;
            }

            boolean isValid = PasswordUtil.matches(password, userDO.getPassword());
            log.debug("密码验证结果，用户ID: {}, 结果: {}", userId, isValid);
            return isValid;

        } catch (Exception e) {
            log.error("验证密码失败，用户ID: {}", userId, e);
            return false;
        }
    }


    @Override
    public String getUserUsername(Long userId) {
        try {
            log.debug("获取用户用户名，用户ID: {}", userId);

            UserDO userDO = userRepository.selectById(userId);
            if (userDO == null) {
                log.warn("用户不存在，用户ID: {}", userId);
                return null;
            }

            return userDO.getUsername();
        } catch (Exception e) {
            log.error("获取用户用户名失败，用户ID: {}", userId, e);
            return null;
        }
    }

    @Override
    public String getUserPhone(Long userId) {
        try {
            log.debug("获取用户手机号，用户ID: {}", userId);

            UserDO userDO = userRepository.selectById(userId);
            if (userDO == null) {
                log.warn("用户不存在，用户ID: {}", userId);
                return null;
            }

            return userDO.getPhone();
        } catch (Exception e) {
            log.error("获取用户手机号失败，用户ID: {}", userId, e);
            return null;
        }
    }

    @Override
    public String getUserEmail(Long userId) {
        try {
            log.debug("获取用户邮箱，用户ID: {}", userId);

            UserDO userDO = userRepository.selectById(userId);
            if (userDO == null) {
                log.warn("用户不存在，用户ID: {}", userId);
                return null;
            }

            return userDO.getEmail();
        } catch (Exception e) {
            log.error("获取用户邮箱失败，用户ID: {}", userId, e);
            return null;
        }
    }

    @Override
    public String getUserNickname(Long userId) {
        try {
            log.debug("获取用户昵称，用户ID: {}", userId);

            UserDO userDO = userRepository.selectById(userId);
            if (userDO == null) {
                log.warn("用户不存在，用户ID: {}", userId);
                return null;
            }

            return userDO.getNickname();
        } catch (Exception e) {
            log.error("获取用户昵称失败，用户ID: {}", userId, e);
            return null;
        }
    }

    @Override
    public String getUserAvatarUrl(Long userId) {
        try {
            log.debug("获取用户头像URL，用户ID: {}", userId);

            UserDO userDO = userRepository.selectById(userId);
            if (userDO == null) {
                log.warn("用户不存在，用户ID: {}", userId);
                return null;
            }

            return userDO.getAvatarUrl();
        } catch (Exception e) {
            log.error("获取用户头像URL失败，用户ID: {}", userId, e);
            return null;
        }
    }

    @Override
    public boolean isUserAccountActive(Long userId) {
        try {
            log.debug("检查用户账户状态，用户ID: {}", userId);

            UserDO userDO = userRepository.selectById(userId);
            if (userDO == null) {
                log.warn("用户不存在，用户ID: {}", userId);
                return false;
            }

            // 检查用户状态是否为正常状态
            boolean isActive = userDO.getStatus() != null && userDO.getStatus().equals(UserStatus.NORMAL.getCode());
            log.debug("用户账户状态检查结果，用户ID: {}, 状态: {}, 是否激活: {}", userId, userDO.getStatus(), isActive);

            return isActive;
        } catch (Exception e) {
            log.error("检查用户账户状态失败，用户ID: {}", userId, e);
            return false;
        }
    }


    /**
     * 生成数字验证码
     */
    private String generateSmsCode() {
        Random random = new Random();
        int min = (int) Math.pow(10, SMS_CODE_LENGTH - 1);
        int max = (int) Math.pow(10, SMS_CODE_LENGTH) - 1;
        int code = min + random.nextInt(max - min + 1);
        return String.valueOf(code);
    }

    /**
     * 将UserDO转换为UserBO
     */
    private UserBO convertToBO(UserDO userDO) {
        return UserConverter.doToBo(userDO);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void deleteUserCompletely(Long userId, Long operatorId) {
        log.warn("开始删除用户及其所有数据，用户ID: {}, 操作人ID: {}", userId, operatorId);

        // 1. 检查操作人是否存在且有效
        if (operatorId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "操作人ID不能为空");
        }
        UserDO operatorDO = userRepository.selectById(operatorId);
        if (operatorDO == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "操作人不存在");
        }

        // 2. 安全检查：操作人必须是SUPER_ADMIN
        List<String> operatorRoles = getUserRoles(operatorId);
        if (operatorRoles == null || !operatorRoles.contains("SUPER_ADMIN")) {
            log.error("非SUPER_ADMIN用户尝试删除用户，操作人ID: {}, 目标用户ID: {}", operatorId, userId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有超级管理员可以删除用户");
        }

        // 3. 检查用户是否存在
        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS, "用户不存在");
        }

        // 4. 安全检查：防止删除自己
        if (userId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除自己的账户");
        }

        // 5. 安全检查：不能删除SUPER_ADMIN用户
        List<String> targetUserRoles = getUserRoles(userId);
        if (targetUserRoles != null && targetUserRoles.contains("SUPER_ADMIN")) {
            log.error("尝试删除SUPER_ADMIN用户，操作人ID: {}, 目标用户ID: {}", operatorId, userId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能删除超级管理员账户");
        }

        // 6. 记录用户完整数据（用于数据恢复）
        Map<String, Object> deleteAuditLog = new HashMap<>();
        deleteAuditLog.put("operation", "DELETE_USER_COMPLETELY");
        deleteAuditLog.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        deleteAuditLog.put("operatorId", operatorId);
        deleteAuditLog.put("operatorPhone", operatorDO.getPhone());
        deleteAuditLog.put("operatorUsername", operatorDO.getUsername());
        deleteAuditLog.put("targetUserId", userId);
        
        // 记录用户基本信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", userDO.getId());
        userInfo.put("username", userDO.getUsername());
        userInfo.put("nickname", userDO.getNickname());
        userInfo.put("phone", userDO.getPhone());
        userInfo.put("email", userDO.getEmail());
        userInfo.put("avatarUrl", userDO.getAvatarUrl());
        userInfo.put("status", userDO.getStatus());
        userInfo.put("roles", userDO.getRoles());
        userInfo.put("createdAt", userDO.getCreatedAt() != null ? userDO.getCreatedAt().toString() : null);
        userInfo.put("updatedAt", userDO.getUpdatedAt() != null ? userDO.getUpdatedAt().toString() : null);
        deleteAuditLog.put("userInfo", userInfo);
        
        // 记录用户角色
        deleteAuditLog.put("userRoles", targetUserRoles);
        
        // 输出完整的删除审计日志（JSON格式，便于后续恢复）
        String auditLogJson = JsonUtil.toJson(deleteAuditLog);
        log.error("===========================================");
        log.error("用户删除审计日志 - 完整数据备份（用于数据恢复）");
        log.error("用户ID: {}, 操作人ID: {}, 操作时间: {}", userId, operatorId, LocalDateTime.now());
        log.error("===========================================");
        log.error("DELETE_USER_AUDIT_LOG_START");
        log.error(auditLogJson);
        log.error("DELETE_USER_AUDIT_LOG_END");
        log.error("===========================================");
        
        log.warn("准备删除用户，用户ID: {}, 手机号: {}, 操作人ID: {}", 
            userId, userDO.getPhone(), operatorId);

        // 7. 开始执行删除操作
        log.warn("开始执行删除操作，用户ID: {}, 操作人ID: {}", userId, operatorId);
        

        // 强制用户下线
        try {
            forceUserOffline(userId);
            log.warn("已强制用户下线，用户ID: {}", userId);
        } catch (Exception e) {
            log.warn("强制用户下线失败，用户ID: {}", userId, e);
        }

        // 最后删除用户
        int userDeleted = userRepository.deleteById(userId);
        log.warn("已删除用户记录，用户ID: {}, 删除数量: {}", userId, userDeleted);

        // 记录删除完成日志
        log.error("===========================================");
        log.error("用户删除操作完成");
        log.error("用户ID: {}, 操作人ID: {}, 完成时间: {}", userId, operatorId, LocalDateTime.now());
        log.error("===========================================");
        
        log.warn("用户及其所有数据删除完成，用户ID: {}, 操作人ID: {}, 操作人手机号: {}", 
            userId, operatorId, operatorDO.getPhone());
    }

}