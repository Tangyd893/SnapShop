package com.snapshop.inventory.controller;

import com.snapshop.common.base.R;
import com.snapshop.inventory.dto.InventoryDeductDTO;
import com.snapshop.inventory.dto.InventoryRecoverDTO;
import com.snapshop.inventory.dto.InventoryResponseDTO;
import com.snapshop.inventory.entity.StockRecord;
import com.snapshop.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存服务内部接口
 */
@RestController
@RequestMapping("/internal/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 查询库存
     */
    @GetMapping("/stock/{skuId}")
    public R<StockRecord> getStock(@PathVariable Long skuId) {
        StockRecord stock = inventoryService.getStock(skuId);
        return R.ok(stock);
    }

    /**
     * 扣减库存
     */
    @PostMapping("/deduct")
    public R<InventoryResponseDTO> deduct(@RequestBody InventoryDeductDTO dto) {
        InventoryResponseDTO result = inventoryService.deductStock(dto);
        return R.ok(result);
    }

    /**
     * 回补库存
     */
    @PostMapping("/recover")
    public R<InventoryResponseDTO> recover(@RequestBody InventoryRecoverDTO dto) {
        InventoryResponseDTO result = inventoryService.recoverStock(dto);
        return R.ok(result);
    }
}
