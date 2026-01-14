package com.lvxin.base.web.aspect;

import com.lvxin.base.service.TokenService;
import com.lvxin.base.util.CurrentUserUtil;
import com.lvxin.base.web.annotation.AuthCheck;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

import static com.lvxin.base.util.CurrentUserUtil.parseAuthorizationHeader;

/**
 * 权限校验切面
 */
@Slf4j
@Aspect
@Component
public class AuthCheckAspect {

    @Resource
    private TokenService tokenService;

    @Value("${security.token.header:Authorization}")
    private String tokenHeader;

    @Around("@annotation(authCheck) || @within(authCheck)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 0) 解析生效的注解（方法优先，其次类），避免authCheck为null导致NPE
        AuthCheck effectiveAuthCheck = resolveAuthCheck(joinPoint, authCheck);
        // 1) 获取请求头中的代理Token
        String proxyToken = getProxyTokenFromRequest();
        if (!StringUtils.hasText(proxyToken)) {
            log.warn("请求头中未找到Token，header={}", tokenHeader);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 2) 登录校验：通过CurrentUserUtil解析Token获取用户信息
        try {
            CurrentUserUtil.getCurrentUser();
        } catch (Exception e) {
            log.warn("登录校验失败", e);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 3) 权限校验：如果有权限点要求，则进行权限校验
        if (effectiveAuthCheck != null && effectiveAuthCheck.value().length > 0) {
            checkPermissions(effectiveAuthCheck);
        }

        // 4) 登录和权限校验通过，续期Token
        try {
            tokenService.refreshToken(proxyToken);
        } catch (Exception e) {
            log.warn("续期Token失败，proxyToken={}", proxyToken, e);
            // 续期失败不影响业务，仅记录日志
        }

        // 5) 执行业务方法
        return joinPoint.proceed();
    }

    /**
     * 解析切点上实际生效的AuthCheck注解：方法优先、其次类级别。若均无，则返回null。
     */
    private AuthCheck resolveAuthCheck(ProceedingJoinPoint joinPoint, AuthCheck authCheck) {
        if (authCheck != null) {
            return authCheck;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            java.lang.reflect.Method method = signature.getMethod();
            AuthCheck methodAnno = method.getAnnotation(AuthCheck.class);
            if (methodAnno != null) {
                return methodAnno;
            }
            Class<?> targetClass = joinPoint.getTarget().getClass();
            return targetClass.getAnnotation(AuthCheck.class);
        } catch (Exception e) {
            // 解析失败则视为无注解，仅做登录校验
            log.debug("解析AuthCheck注解失败，将仅校验登录", e);
            return null;
        }
    }

    private String getProxyTokenFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String raw = request.getHeader(tokenHeader);
        String parsed = parseAuthorizationHeader(raw);
        try {
            String altHeader = request.getHeader("token");
            String altParam = request.getParameter("token");
            if (log.isDebugEnabled()) {
                log.debug("Auth header '{}' raw='{}', parsedPresent={}, altHeaderPresent={}, altParamPresent={}",
                        tokenHeader,
                        raw,
                        parsed != null,
                        altHeader != null,
                        altParam != null);
            }
        } catch (Exception ignore) {
        }
        return parsed;
    }

    private void checkPermissions(AuthCheck authCheck) {
        String[] requiredPermissions = authCheck.value();
        AuthCheck.MatchMode matchMode = authCheck.matchMode();

        if (requiredPermissions.length == 0) {
            return;
        }

        boolean hasPermission;
        if (matchMode == AuthCheck.MatchMode.ALL) {
            // 全部命中模式 - 使用CurrentUserUtil的hasAllPermissions方法
            hasPermission = CurrentUserUtil.hasAllPermissions(requiredPermissions);
        } else {
            // 任一命中模式 - 使用CurrentUserUtil的hasAnyPermission方法
            hasPermission = CurrentUserUtil.hasAnyPermission(requiredPermissions);
        }

        if (!hasPermission) {
            try {
                Long userId = CurrentUserUtil.getCurrentUserId();
                log.warn("权限校验失败，用户ID={}，所需权限={}，匹配模式={}",
                        userId, Arrays.toString(requiredPermissions), matchMode);
            } catch (Exception e) {
                log.warn("权限校验失败，所需权限={}，匹配模式={}，用户信息获取失败",
                        Arrays.toString(requiredPermissions), matchMode, e);
            }
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        try {
            Long userId = CurrentUserUtil.getCurrentUserId();
            log.debug("权限校验通过，用户ID={}，所需权限={}，匹配模式={}",
                    userId, Arrays.toString(requiredPermissions), matchMode);
        } catch (Exception e) {
            log.debug("权限校验通过，所需权限={}，匹配模式={}，用户信息获取失败",
                    Arrays.toString(requiredPermissions), matchMode, e);
        }
    }
}
