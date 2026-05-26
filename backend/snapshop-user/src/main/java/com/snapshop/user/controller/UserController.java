package com.snapshop.user.controller;

import com.snapshop.common.base.R;
import com.snapshop.user.dto.CreateAddressDTO;
import com.snapshop.user.dto.UpdateProfileDTO;
import com.snapshop.user.service.UserService;
import com.snapshop.user.vo.UserAddressVO;
import com.snapshop.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务接口
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户资料
     */
    @GetMapping("/me")
    public R<UserProfileVO> getProfile(@RequestHeader("X-User-Id") Long userId) {
        UserProfileVO profile = userService.getProfile(userId);
        return R.ok(profile);
    }

    /**
     * 更新当前用户资料
     */
    @PutMapping("/me")
    public R<UserProfileVO> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody UpdateProfileDTO dto) {
        UserProfileVO profile = userService.updateProfile(userId, dto);
        return R.ok(profile);
    }

    /**
     * 获取当前用户收货地址列表
     */
    @GetMapping("/addresses")
    public R<List<UserAddressVO>> getAddresses(@RequestHeader("X-User-Id") Long userId) {
        List<UserAddressVO> addresses = userService.getAddresses(userId);
        return R.ok(addresses);
    }

    /**
     * 新增收货地址
     */
    @PostMapping("/addresses")
    public R<UserAddressVO> addAddress(@RequestHeader("X-User-Id") Long userId,
                                       @RequestBody CreateAddressDTO dto) {
        UserAddressVO address = userService.addAddress(userId, dto);
        return R.ok(address);
    }
}
