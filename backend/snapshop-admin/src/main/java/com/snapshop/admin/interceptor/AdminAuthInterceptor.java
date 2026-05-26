package com.snapshop.admin.interceptor;

import com.snapshop.admin.annotation.RequireRole;
import com.snapshop.admin.util.AdminJwtUtil;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * 管理后台认证拦截器，校验 admin JWT 和 RBAC 角色
 */
@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Resource
    private AdminJwtUtil adminJwtUtil;

    private static final String ADMIN_ID_ATTR = "adminId";
    private static final String ADMIN_USERNAME_ATTR = "adminUsername";
    private static final String ADMIN_ROLE_ATTR = "adminRole";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "缺少管理后台令牌");
        }

        String token = authHeader.substring(7);
        if (!adminJwtUtil.validateToken(token)) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "管理后台令牌无效或已过期");
        }

        Long adminId = adminJwtUtil.getAdminId(token);
        String role = adminJwtUtil.getRole(token);
        String username = adminJwtUtil.getUsername(token);

        if (adminId == null || role == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "令牌解析失败");
        }

        request.setAttribute(ADMIN_ID_ATTR, adminId);
        request.setAttribute(ADMIN_USERNAME_ATTR, username);
        request.setAttribute(ADMIN_ROLE_ATTR, role);

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole != null) {
            List<String> allowedRoles = Arrays.asList(requireRole.value());
            if (!allowedRoles.contains(role)) {
                throw new BizException(ErrorCode.FORBIDDEN, "权限不足，需要角色: " + String.join(",", allowedRoles));
            }
        }

        return true;
    }
}
