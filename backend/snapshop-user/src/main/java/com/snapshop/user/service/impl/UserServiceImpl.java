package com.snapshop.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.user.dto.CreateAddressDTO;
import com.snapshop.user.dto.UpdateProfileDTO;
import com.snapshop.user.entity.User;
import com.snapshop.user.entity.UserAddress;
import com.snapshop.user.mapper.UserAddressMapper;
import com.snapshop.user.mapper.UserMapper;
import com.snapshop.user.service.UserService;
import com.snapshop.user.vo.UserAddressVO;
import com.snapshop.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserAddressMapper userAddressMapper;

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = getUserById(userId);
        return buildUserProfileVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = getUserById(userId);

        // 更新昵称（不为空时）
        if (dto.getNickname() != null && !dto.getNickname().isEmpty()) {
            user.setNickname(dto.getNickname());
        }
        // 更新头像（不为空时）
        if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().isEmpty()) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户资料更新成功: userId={}", userId);
        return buildUserProfileVO(user);
    }

    @Override
    public List<UserAddressVO> getAddresses(Long userId) {
        // 验证用户存在
        getUserById(userId);

        LambdaQueryWrapper<UserAddress> queryWrapper = new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getDefaultAddress)
                .orderByDesc(UserAddress::getCreatedAt);

        List<UserAddress> addresses = userAddressMapper.selectList(queryWrapper);
        return addresses.stream().map(this::buildUserAddressVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddressVO addAddress(Long userId, CreateAddressDTO dto) {
        // 验证用户存在
        getUserById(userId);

        // 如果设置为默认地址，先取消其他默认地址
        if (Boolean.TRUE.equals(dto.getDefaultAddress())) {
            cancelDefaultAddress(userId);
        }

        // 创建新地址
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetailAddress(dto.getDetailAddress());
        address.setDefaultAddress(dto.getDefaultAddress() != null ? dto.getDefaultAddress() : false);

        LocalDateTime now = LocalDateTime.now();
        address.setCreatedAt(now);
        address.setUpdatedAt(now);

        userAddressMapper.insert(address);
        log.info("收货地址创建成功: userId={}, addressId={}", userId, address.getId());

        return buildUserAddressVO(address);
    }

    /**
     * 根据用户编号获取用户信息
     */
    private User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return user;
    }

    /**
     * 取消用户的所有默认地址
     */
    private void cancelDefaultAddress(Long userId) {
        LambdaQueryWrapper<UserAddress> queryWrapper = new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getDefaultAddress, true);

        List<UserAddress> defaultAddresses = userAddressMapper.selectList(queryWrapper);
        for (UserAddress addr : defaultAddresses) {
            addr.setDefaultAddress(false);
            addr.setUpdatedAt(LocalDateTime.now());
            userAddressMapper.updateById(addr);
        }
    }

    /**
     * 构建用户资料 VO
     */
    private UserProfileVO buildUserProfileVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setStatus(user.getStatus());
        return vo;
    }

    /**
     * 构建用户地址 VO
     */
    private UserAddressVO buildUserAddressVO(UserAddress address) {
        UserAddressVO vo = new UserAddressVO();
        vo.setAddressId(address.getId());
        vo.setReceiverName(address.getReceiverName());
        vo.setReceiverPhone(address.getReceiverPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetailAddress(address.getDetailAddress());
        vo.setDefaultAddress(address.getDefaultAddress());
        return vo;
    }
}
