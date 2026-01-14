package com.lvxin.base.util;

import com.lvxin.base.constant.UserStatus;
import com.lvxin.base.service.TokenService;
import com.lvxin.base.service.bo.UserOnlineInfoBO;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

/**
 * 当前用户工具类
 * 提供静态方法获取当前请求的用户信息
 */
@Slf4j
@Component
public class CurrentUserUtil {

    @Resource
    private TokenService tokenService;

    @Value("${security.token.header:Authorization}")
    private String tokenHeader;

    private static CurrentUserUtil instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果未登录则抛出异常
     */
    public static Long getCurrentUserId() {
        UserOnlineInfoBO onlineInfo = getCurrentUser();
        return onlineInfo.getId();
    }

    /**
     * 获取当前用户在线信息
     *
     * @return 用户在线信息，如果未登录则抛出异常
     */
    public static UserOnlineInfoBO getCurrentUser() {
        if (instance == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "CurrentUserUtil未初始化");
        }
        return instance.getCurrentUserInternal();
    }

    /**
     * 获取当前用户权限集合
     *
     * @return 权限集合，如果未登录则抛出异常
     */
    public static Set<String> getCurrentUserPermissions() {
        UserOnlineInfoBO onlineInfo = getCurrentUser();
        return onlineInfo.getPermissionCodeSet();
    }

    /**
     * 检查当前用户是否拥有指定权限
     *
     * @param permission 权限点
     * @return true 拥有权限，false 不拥有权限
     */
    public static boolean hasPermission(String permission) {
        try {
            Set<String> permissions = getCurrentUserPermissions();
            return permissions.contains(permission);
        } catch (Exception e) {
            log.debug("检查权限失败，用户未登录或权限获取失败", e);
            return false;
        }
    }

    /**
     * 检查当前用户是否拥有任一权限
     *
     * @param permissions 权限点数组
     * @return true 拥有任一权限，false 不拥有任何权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        try {
            Set<String> userPermissions = getCurrentUserPermissions();
            for (String permission : permissions) {
                if (userPermissions.contains(permission)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("检查权限失败，用户未登录或权限获取失败", e);
            return false;
        }
    }

    /**
     * 检查当前用户是否拥有所有权限
     *
     * @param permissions 权限点数组
     * @return true 拥有所有权限，false 缺少任一权限
     */
    public static boolean hasAllPermissions(String... permissions) {
        try {
            Set<String> userPermissions = getCurrentUserPermissions();
            if (userPermissions.contains("*")) {
                log.info("超管拥有所有权限");
                return true;
            }
            for (String permission : permissions) {
                if (!userPermissions.contains(permission)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.debug("检查权限失败，用户未登录或权限获取失败", e);
            return false;
        }
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果未登录则抛出异常
     */
    public static String getCurrentUsername() {
        UserOnlineInfoBO onlineInfo = getCurrentUser();
        return onlineInfo.getUsername();
    }

    /**
     * 获取当前用户昵称
     *
     * @return 用户昵称，如果未登录则抛出异常
     */
    public static String getCurrentUserNickname() {
        UserOnlineInfoBO onlineInfo = getCurrentUser();
        return onlineInfo.getNickname();
    }

    /**
     * 获取当前用户手机号
     *
     * @return 手机号，如果未登录则抛出异常
     */
    public static String getCurrentUserPhone() {
        UserOnlineInfoBO onlineInfo = getCurrentUser();
        return onlineInfo.getPhone();
    }

    /**
     * 获取当前用户邮箱
     *
     * @return 邮箱，如果未登录则抛出异常
     */
    public static String getCurrentUserEmail() {
        UserOnlineInfoBO onlineInfo = getCurrentUser();
        return onlineInfo.getEmail();
    }

    /**
     * 获取当前用户角色列表
     *
     * @return 角色列表，如果未登录则抛出异常
     */
    public static java.util.List<String> getCurrentUserRoles() {
        UserOnlineInfoBO onlineInfo = getCurrentUser();
        return onlineInfo.getRoles();
    }

    /**
     * 检查当前用户账户是否激活
     *
     * @return true 账户激活，false 账户未激活
     */
    public static boolean isCurrentAccountActive() {
        try {
            UserOnlineInfoBO onlineInfo = getCurrentUser();
            // 这里可以根据实际业务逻辑检查账户状态
            // 例如：检查用户状态、是否被锁定等
            return onlineInfo.getStatus() != null && onlineInfo.getStatus() == UserStatus.NORMAL;
        } catch (Exception e) {
            log.debug("检查账户状态失败", e);
            return false;
        }
    }

    /**
     * 检查指定用户账户是否激活
     *
     * @param userId 用户ID
     * @return true 账户激活，false 账户未激活
     */
    public static boolean isAccountActive(Long userId) {
        try {
            // 如果是当前用户，直接使用当前用户信息
            if (userId.equals(getCurrentUserId())) {
                return isCurrentAccountActive();
            }

            // TODO: 实现查询指定用户账户状态的逻辑
            // 这里需要注入UserService或UserRepository来查询用户状态
            log.debug("检查指定用户账户状态，用户ID: {}", userId);
            return true; // 模拟账户激活
        } catch (Exception e) {
            log.error("检查账户状态失败，用户ID: {}", userId, e);
            return false;
        }
    }

    /**
     * 检查当前用户是否拥有指定角色
     *
     * @param role 角色名
     * @return true 拥有角色，false 不拥有角色
     */
    public static boolean hasRole(String role) {
        try {
            java.util.List<String> roles = getCurrentUserRoles();
            return roles.contains(role);
        } catch (Exception e) {
            log.debug("检查角色失败，用户未登录或角色获取失败", e);
            return false;
        }
    }

    /**
     * 检查当前用户是否拥有任一角色
     *
     * @param roles 角色名数组
     * @return true 拥有任一角色，false 不拥有任何角色
     */
    public static boolean hasAnyRole(String... roles) {
        try {
            java.util.List<String> userRoles = getCurrentUserRoles();
            for (String role : roles) {
                if (userRoles.contains(role)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("检查角色失败，用户未登录或角色获取失败", e);
            return false;
        }
    }

    /**
     * 检查当前用户是否拥有所有角色
     *
     * @param roles 角色名数组
     * @return true 拥有所有角色，false 缺少任一角色
     */
    public static boolean hasAllRoles(String... roles) {
        try {
            java.util.List<String> userRoles = getCurrentUserRoles();
            for (String role : roles) {
                if (!userRoles.contains(role)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.debug("检查角色失败，用户未登录或角色获取失败", e);
            return false;
        }
    }

    /**
     * 内部方法：获取当前用户信息
     *
     * @return 用户在线信息
     */
    private UserOnlineInfoBO getCurrentUserInternal() {
        // 1) 获取请求头中的代理Token
        String proxyToken = getProxyTokenFromRequest();
        if (log.isDebugEnabled()) {
            log.debug("CurrentUserUtil resolved proxyToken present={}", StringUtils.hasText(proxyToken));
        }
        if (!StringUtils.hasText(proxyToken)) {
            log.warn("请求头中未找到Token，header={}", tokenHeader);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2) 解析用户在线信息
        try {
            return tokenService.getUserOnlineInfo(proxyToken);
        } catch (Exception e) {
            log.warn("解析Token失败，proxyToken={}", proxyToken, e);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 从请求中获取代理Token
     *
     * @return 代理Token
     */
    private String getProxyTokenFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return extractProxyToken(request);
    }

    /**
     * 解析 Authorization 请求头中的代理 Token，兼容 "Bearer <token>" 与裸 token。
     *
     * @param rawHeader 原始Authorization值
     * @return 提取出的token；若为空或无效返回null
     */
    public static String parseAuthorizationHeader(String rawHeader) {
        if (rawHeader == null) {
            return null;
        }
        String val = rawHeader.trim();
        if (val.regionMatches(true, 0, "Bearer", 0, 6)) {
            val = val.substring(6).trim();
        }
        return val.isEmpty() ? null : val;
    }

    /**
     * 从请求中提取代理Token，支持以下来源（按优先级）：
     * 1) 配置的tokenHeader（默认Authorization），支持Bearer前缀
     * 2) 备用请求头：token（不区分大小写）
     * 3) 查询参数：token
     */
    public static String extractProxyToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // 1) 主请求头：配置的tokenHeader（如Authorization）
        String primary = request.getHeader(instance != null ? instance.tokenHeader : "Authorization");
        String token = parseAuthorizationHeader(primary);
        if (log.isDebugEnabled()) {
            log.debug("extractProxyToken primaryHeader='{}' present={}, parsedPresent={}",
                    instance != null ? instance.tokenHeader : "Authorization",
                    primary != null,
                    StringUtils.hasText(token));
        }
        if (StringUtils.hasText(token)) {
            return token;
        }
        // 2) 备用请求头：token（大小写不敏感）
        String alt = request.getHeader("token");
        token = parseAuthorizationHeader(alt);
        if (log.isDebugEnabled()) {
            log.debug("extractProxyToken altHeader present={}, parsedPresent={}", alt != null, StringUtils.hasText(token));
        }
        if (StringUtils.hasText(token)) {
            return token;
        }
        String altUpper = request.getHeader("Token");
        token = parseAuthorizationHeader(altUpper);
        if (log.isDebugEnabled()) {
            log.debug("extractProxyToken altHeaderUpper present={}, parsedPresent={}", altUpper != null, StringUtils.hasText(token));
        }
        if (StringUtils.hasText(token)) {
            return token;
        }
        // 3) 查询参数：token
        String param = request.getParameter("token");
        token = parseAuthorizationHeader(param);
        if (log.isDebugEnabled()) {
            log.debug("extractProxyToken param present={}, parsedPresent={}", param != null, StringUtils.hasText(token));
        }
        return StringUtils.hasText(token) ? token : null;
    }

    /**
     * 获取客户端IP（从当前请求上下文）
     * 优先解析反向代理/负载均衡常见头：X-Forwarded-For、X-Real-IP 等
     *
     * @return 客户端IP，获取失败返回null
     */
    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return getClientIp(attributes.getRequest());
    }

    /**
     * 从 HttpServletRequest 获取客户端IP
     *
     * @param request HttpServletRequest
     * @return 客户端IP，获取失败返回null
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 常见代理头按优先级依次解析
        String[] headerNames = new String[]{
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个IP，取第一个非unknown
                if (ip.contains(",")) {
                    String[] parts = ip.split(",");
                    for (String part : parts) {
                        String candidate = part.trim();
                        if (StringUtils.hasText(candidate) && !"unknown".equalsIgnoreCase(candidate)) {
                            return candidate;
                        }
                    }
                }
                return ip.trim();
            }
        }

        // 兜底：直接使用远端地址
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : null;
    }

    /**
     * 获取客户端User-Agent（从当前请求上下文）
     *
     * @return User-Agent字符串，获取失败返回null
     */
    public static String getUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return getUserAgent(attributes.getRequest());
    }

    /**
     * 从 HttpServletRequest 获取User-Agent
     *
     * @param request HttpServletRequest
     * @return User-Agent字符串，获取失败返回null
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String userAgent = request.getHeader("User-Agent");
        return StringUtils.hasText(userAgent) ? userAgent : null;
    }

    /**
     * 解析设备信息
     *
     * @return 设备信息字符串
     */
    public static String getDeviceInfo() {
        String userAgent = getUserAgent();
        if (userAgent == null) {
            return "Unknown Device";
        }
        return parseDeviceInfo(userAgent);
    }

    /**
     * 从User-Agent解析设备信息
     *
     * @param userAgent User-Agent字符串
     * @return 设备信息
     */
    public static String parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "Unknown Device";
        }

        // 移动设备检测
        if (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone")) {
            return parseMobileDevice(userAgent);
        }

        // 桌面设备检测
        if (userAgent.contains("Windows") || userAgent.contains("Macintosh") || userAgent.contains("Linux")) {
            return parseDesktopDevice(userAgent);
        }

        // 其他设备
        return "Other Device";
    }

    /**
     * 解析移动设备信息
     *
     * @param userAgent User-Agent字符串
     * @return 移动设备信息
     */
    private static String parseMobileDevice(String userAgent) {
        // iPhone检测
        if (userAgent.contains("iPhone")) {
            String version = extractVersion(userAgent, "iPhone OS (\\d+)_(\\d+)");
            return version != null ? "iPhone " + version : "iPhone";
        }

        // Android检测
        if (userAgent.contains("Android")) {
            String version = extractVersion(userAgent, "Android (\\d+\\.\\d+)");
            String device = extractDevice(userAgent);
            return version != null ? device + " Android " + version : device;
        }

        // iPad检测
        if (userAgent.contains("iPad")) {
            String version = extractVersion(userAgent, "OS (\\d+)_(\\d+)");
            return version != null ? "iPad " + version : "iPad";
        }

        return "Mobile Device";
    }

    /**
     * 解析桌面设备信息
     *
     * @param userAgent User-Agent字符串
     * @return 桌面设备信息
     */
    private static String parseDesktopDevice(String userAgent) {
        if (userAgent.contains("Windows")) {
            if (userAgent.contains("Windows NT 10.0")) {
                return "Windows 10";
            } else if (userAgent.contains("Windows NT 6.3")) {
                return "Windows 8.1";
            } else if (userAgent.contains("Windows NT 6.1")) {
                return "Windows 7";
            }
            return "Windows";
        }

        if (userAgent.contains("Macintosh")) {
            return "Mac OS X";
        }

        if (userAgent.contains("Linux")) {
            return "Linux";
        }

        return "Desktop";
    }

    /**
     * 提取版本号
     *
     * @param userAgent User-Agent字符串
     * @param pattern   正则表达式
     * @return 版本号
     */
    private static String extractVersion(String userAgent, String pattern) {
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(userAgent);
            if (m.find()) {
                if (m.groupCount() >= 2) {
                    return m.group(1) + "." + m.group(2);
                } else if (m.groupCount() >= 1) {
                    return m.group(1);
                }
            }
        } catch (Exception e) {
            log.debug("提取版本号失败", e);
        }
        return null;
    }

    /**
     * 提取设备名称
     *
     * @param userAgent User-Agent字符串
     * @return 设备名称
     */
    private static String extractDevice(String userAgent) {
        // 常见Android设备检测
        if (userAgent.contains("SM-")) {
            return "Samsung Galaxy";
        }
        if (userAgent.contains("Pixel")) {
            return "Google Pixel";
        }
        if (userAgent.contains("OnePlus")) {
            return "OnePlus";
        }
        if (userAgent.contains("HUAWEI")) {
            return "Huawei";
        }
        if (userAgent.contains("Xiaomi")) {
            return "Xiaomi";
        }

        return "Android Device";
    }
}
