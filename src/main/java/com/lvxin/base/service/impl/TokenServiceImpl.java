package com.lvxin.base.service.impl;

import com.lvxin.base.service.TokenService;
import com.lvxin.base.service.bo.UserOnlineInfoBO;
import com.lvxin.base.constant.UserStatus;
import com.lvxin.base.repository.UserRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lvxin.base.repository.RoleRepository;
import com.lvxin.base.repository.entity.RoleDO;
import com.lvxin.base.repository.entity.UserDO;
import com.lvxin.base.service.RedisService;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import com.lvxin.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Token管理服务实现类
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    /**
     * 代理Token与完整Token的Redis前缀
     */
    private static final String REDIS_KEY_PREFIX = "auth:token:";

    /**
     * 用户Token映射的Redis前缀
     */
    private static final String USER_TOKEN_PREFIX = "auth:user_tokens:";

    @Resource
    private UserRepository userRepository;

    @Resource
    private RedisService redisService;

    @Resource
    private RoleRepository roleRepository;

    @Value("${security.token.secret:change-me-please-change-me-please-32bytes-min}")
    private String jwtSecret;

    @Value("${security.token.ttl:7d}")
    private Duration tokenTtl;

    private Key getJwtSignKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String createToken(Long userId) {

        // 1) 查询用户
        UserDO userDO = userRepository.selectById(userId);
        if (userDO == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        // 2) 组装在线信息（权限点在当前阶段留空集合）
        UserOnlineInfoBO onlineInfo = toUserOnlineInfoBO(userDO);

        // 3) 生成完整 JWT
        String fullToken = generateJwt(onlineInfo);

        // 4) 生成代理 Token，并缓存映射关系
        String proxyToken = UUID.randomUUID().toString().replace("-", "");
        String tokenKey = REDIS_KEY_PREFIX + proxyToken;
        redisService.set(tokenKey, fullToken, tokenTtl);

        // 5) 维护用户Token映射关系（与Token存储保持同步的TTL）
        String userTokensKey = USER_TOKEN_PREFIX + userId;
        redisService.set(userTokensKey, proxyToken, tokenTtl);

        log.debug("用户 {} 创建Token成功，代理Token: {}", userId, proxyToken);

        // 6) 返回代理 Token
        return proxyToken;
    }

    @Override
    public void refreshToken(String proxyToken) {
        // 1) 根据代理token去缓存中获取完整token
        String redisKey = REDIS_KEY_PREFIX + proxyToken;
        String fullToken = redisService.get(redisKey, String.class);
        if (fullToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2) 解析完整token获取用户ID
        try {
            UserOnlineInfoBO userInfo = parseJwt(fullToken);
            Long userId = userInfo.getId();

            // 3) 刷新代理Token的过期时间
            redisService.expire(redisKey, tokenTtl);

            // 4) 刷新用户Token映射的过期时间（保持同步）
            String userTokensKey = USER_TOKEN_PREFIX + userId;
            redisService.expire(userTokensKey, tokenTtl);

            log.debug("Token刷新成功，用户ID: {}, 代理Token: {}", userId, proxyToken);
        } catch (Exception e) {
            log.warn("刷新Token时解析用户信息失败，仅刷新代理Token: {}", proxyToken, e);
            // 即使解析失败，也要刷新代理Token
            redisService.expire(redisKey, tokenTtl);
        }
    }

    @Override
    public void removeToken(String proxyToken) {
        try {
            // 1) 先获取完整Token
            String tokenKey = REDIS_KEY_PREFIX + proxyToken;
            String fullToken = redisService.get(tokenKey, String.class);

            if (fullToken != null) {
                try {
                    // 2) 解析Token获取用户ID
                    UserOnlineInfoBO userInfo = parseJwt(fullToken);
                    String userTokensKey = USER_TOKEN_PREFIX + userInfo.getId();

                    // 3) 删除Token映射
                    redisService.delete(tokenKey);
                    redisService.delete(userTokensKey);

                    log.info("删除用户 {} 的Token: {}", userInfo.getId(), proxyToken);
                } catch (Exception e) {
                    log.warn("解析Token失败，直接删除Token: {}", proxyToken, e);
                    // 即使解析失败，也要删除Token
                    redisService.delete(tokenKey);
                }
            } else {
                log.debug("Token不存在或已过期: {}", proxyToken);
            }
        } catch (Exception e) {
            log.error("删除Token失败，proxyToken: {}", proxyToken, e);
            // 确保即使出错也要尝试删除Token
            String tokenKey = REDIS_KEY_PREFIX + proxyToken;
            redisService.delete(tokenKey);
        }
    }

    @Override
    public UserOnlineInfoBO getUserOnlineInfo(String proxyToken) {
        // 根据代理token去缓存中获取完整token，使用jwt解析完整token获取用户信息，转化为UserOnlineInfoBO返回
        String redisKey = REDIS_KEY_PREFIX + proxyToken;
        String fullToken = redisService.get(redisKey, String.class);
        if (log.isDebugEnabled()) {
            log.debug("getUserOnlineInfo redisKey='{}' hit={}", redisKey, fullToken != null);
        }
        if (fullToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return parseJwt(fullToken);
    }

    // -------------------- 私有方法 --------------------

    private UserOnlineInfoBO toUserOnlineInfoBO(UserDO userDO) {
        List<String> roles = parseRoles(userDO.getRoles());
        // 根据用户角色聚合权限点（本地查询，避免依赖UserService导致循环依赖）
        List<String> permissions = getPermissionsByRoles(roles);
        UserOnlineInfoBO online = new UserOnlineInfoBO();
        online.setId(userDO.getId());
        online.setUsername(userDO.getUsername());
        online.setPhone(userDO.getPhone());
        online.setEmail(userDO.getEmail());
        online.setNickname(userDO.getNickname());
        online.setAvatarUrl(userDO.getAvatarUrl());
        online.setRoles(roles);
        online.setStatus(UserStatus.fromCode(userDO.getStatus()));
        online.setCreatedAt(userDO.getCreatedAt());
        online.setUpdatedAt(userDO.getUpdatedAt());
        online.setPermissionCodeSet(permissions == null ? Collections.emptySet() : new HashSet<>(permissions));
        return online;
    }

    /**
     * 根据角色编码列表聚合权限点
     */
    private List<String> getPermissionsByRoles(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<RoleDO> roles = roleRepository.selectList(
                new QueryWrapper<RoleDO>().in("role_code", roleCodes)
        );
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .filter(r -> r != null && r.getPermissions() != null)
                .flatMap(r -> JsonUtil.toList(r.getPermissions(), String.class).stream())
                .distinct()
                .toList();
    }

    private List<String> parseRoles(String rolesJson) {
        if (rolesJson == null || rolesJson.isBlank()) {
            return List.of();
        }
        return JsonUtil.toList(rolesJson, String.class);
    }

    private String generateJwt(UserOnlineInfoBO onlineInfo) {
        Instant now = Instant.now();
        Instant exp = now.plus(tokenTtl);
        return Jwts.builder()
                .setSubject(String.valueOf(onlineInfo.getId()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("uid", onlineInfo.getId())
                .claim("username", onlineInfo.getUsername())
                .claim("nickname", onlineInfo.getNickname())
                .claim("avatarUrl", onlineInfo.getAvatarUrl())
                .claim("phone", onlineInfo.getPhone())
                .claim("email", onlineInfo.getEmail())
                .claim("status", onlineInfo.getStatus() == null ? null : onlineInfo.getStatus().getCode())
                .claim("roles", onlineInfo.getRoles())
                .claim("permissions", onlineInfo.getPermissionCodeSet())
                .signWith(getJwtSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private UserOnlineInfoBO parseJwt(String token) {
        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(getJwtSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long id = claims.get("uid", Number.class).longValue();
        String username = claims.get("username", String.class);
        String nickname = claims.get("nickname", String.class);
        String avatarUrl = claims.get("avatarUrl", String.class);
        String phone = claims.get("phone", String.class);
        String email = claims.get("email", String.class);
        Integer status = claims.get("status", Number.class) == null
                ? null : claims.get("status", Number.class).intValue();

        List<String> roles;
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> list) {
            roles = list.stream().map(String::valueOf).toList();
        } else {
            roles = Collections.emptyList();
        }

        // 解析权限点
        Set<String> permissionCodes;
        Object permsObj = claims.get("permissions");
        if (permsObj instanceof List<?> list) {
            permissionCodes = new HashSet<>();
            for (Object o : list) {
                if (o != null) permissionCodes.add(String.valueOf(o));
            }
        } else {
            permissionCodes = Collections.emptySet();
        }

        UserOnlineInfoBO online = new UserOnlineInfoBO();
        online.setId(id);
        online.setUsername(username);
        online.setNickname(nickname);
        online.setAvatarUrl(avatarUrl);
        online.setPhone(phone);
        online.setEmail(email);
        online.setStatus(UserStatus.fromCode(status));
        online.setRoles(roles);
        online.setPermissionCodeSet(permissionCodes);
        return online;
    }

    @Override
    public void invalidateUserTokens(Long userId) {

        // 1) 获取用户的Token
        String userTokensKey = USER_TOKEN_PREFIX + userId;
        String proxyToken = redisService.get(userTokensKey, String.class);

        if (proxyToken == null) {
            log.info("用户 {} 没有活跃的Token", userId);
            return;
        }

        // 2) 删除Token
        String tokenKey = REDIS_KEY_PREFIX + proxyToken;
        redisService.delete(tokenKey);

        // 3) 删除用户Token映射
        redisService.delete(userTokensKey);

        log.info("用户 {} 的Token失效完成，删除Token: {}", userId, proxyToken);
    }

    @Override
    public boolean hasValidToken(Long userId) {

        // 1) 检查用户Token映射是否存在
        String userTokensKey = USER_TOKEN_PREFIX + userId;
        if (!redisService.exists(userTokensKey)) {
            log.debug("用户 {} 没有Token映射", userId);
            return false;
        }

        // 2) 获取用户的Token
        String proxyToken = redisService.get(userTokensKey, String.class);
        if (proxyToken == null) {
            log.debug("用户 {} 的Token为空", userId);
            return false;
        }

        // 3) 检查Token是否有效
        String tokenKey = REDIS_KEY_PREFIX + proxyToken;
        if (redisService.exists(tokenKey)) {
            log.debug("用户 {} 存在有效Token: {}", userId, proxyToken);
            return true;
        }

        // 4) 清理无效的Token引用
        redisService.delete(userTokensKey);
        log.debug("用户 {} 没有有效Token，已清理Token映射", userId);
        return false;
    }
}