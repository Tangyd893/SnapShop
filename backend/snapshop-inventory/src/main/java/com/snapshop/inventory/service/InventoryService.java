package com.snapshop.inventory.service;

import com.snapshop.inventory.dto.InventoryDeductDTO;
import com.snapshop.inventory.dto.InventoryRecoverDTO;
import com.snapshop.inventory.dto.InventoryResponseDTO;
import com.snapshop.inventory.entity.StockRecord;

/**
 * 库存服务接口
 */
public interface InventoryService {

    /**
     * 查询库存
     *
     * @param skuId 商品规格编号
     * @return 库存记录
     */
    StockRecord getStock(Long skuId);

    /**
     * 扣减库存
     *
     * @param dto 扣减请求
     * @return 扣减响应
     */
    InventoryResponseDTO deductStock(InventoryDeductDTO dto);

    /**
     * 回补库存
     *
     * @param dto 回补请求
     * @return 回补响应
     */
    InventoryResponseDTO recoverStock(InventoryRecoverDTO dto);
}
