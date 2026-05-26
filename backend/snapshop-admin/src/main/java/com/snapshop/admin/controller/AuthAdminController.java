package com.snapshop.admin.controller;

import com.snapshop.admin.service.AdminUserService;
import com.snapshop.admin.vo.AdminLoginDTO;
import com.snapshop.admin.vo.AdminLoginVO;
import com.snapshop.common.base.R;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台认证控制器
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AuthAdminController {

    @Resource
    private AdminUserService adminUserService;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public R<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        AdminLoginVO vo = adminUserService.login(dto.getUsername(), dto.getPassword());
        return R.ok(vo);
    }

    /**
     * 管理员退出（令牌在拦截器中已校验）
     */
    @PostMapping("/logout")
    public R<Boolean> logout() {
        return R.ok(true);
    }
}
