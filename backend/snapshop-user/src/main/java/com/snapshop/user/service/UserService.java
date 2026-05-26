package com.snapshop.user.service;

import com.snapshop.user.dto.CreateAddressDTO;
import com.snapshop.user.dto.UpdateProfileDTO;
import com.snapshop.user.vo.UserAddressVO;
import com.snapshop.user.vo.UserProfileVO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取用户资料
     *
     * @param userId 用户编号
     * @return 用户资料
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 更新用户资料
     *
     * @param userId 用户编号
     * @param dto    更新参数
     * @return 更新后的用户资料
     */
    UserProfileVO updateProfile(Long userId, UpdateProfileDTO dto);

    /**
     * 获取用户收货地址列表
     *
     * @param userId 用户编号
     * @return 地址列表
     */
    List<UserAddressVO> getAddresses(Long userId);

    /**
     * 新增收货地址
     *
     * @param userId 用户编号
     * @param dto    地址信息
     * @return 新增的地址
     */
    UserAddressVO addAddress(Long userId, CreateAddressDTO dto);
}
