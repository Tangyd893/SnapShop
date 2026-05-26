package com.snapshop.admin.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RBAC 权限注解，标注接口所需角色
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /** 允许的角色列表，如 SUPER_ADMIN / OPERATOR / SUPPORT */
    String[] value();
}
