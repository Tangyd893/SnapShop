package com.snapshop.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.snapshop.admin.entity.AdminUser;
import com.snapshop.admin.mapper.AdminUserMapper;
import com.snapshop.admin.service.AdminUserService;
import com.snapshop.admin.util.AdminJwtUtil;
import com.snapshop.admin.util.PasswordUtil;
import com.snapshop.admin.vo.AdminLoginVO;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private PasswordUtil passwordUtil;

    @Resource
    private AdminJwtUtil adminJwtUtil;

    @Override
    public AdminLoginVO login(String username, String password) {
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminUser::getUsername, username);
        AdminUser adminUser = adminUserMapper.selectOne(wrapper);

        if (adminUser == null) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误");
        }

        if ("DISABLED".equals(adminUser.getStatus())) {
            throw new BizException(ErrorCode.USER_DISABLED, "管理员账号已被禁用");
        }

        if (!passwordUtil.matches(password, adminUser.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误");
        }

        String token = adminJwtUtil.generateAccessToken(adminUser.getId(), adminUser.getUsername(), adminUser.getRole());

        return AdminLoginVO.of(token, adminJwtUtil.getAccessTokenTtl(),
                adminUser.getId(), adminUser.getUsername(), adminUser.getRole());
    }
}
