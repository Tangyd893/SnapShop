package com.snapshop.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.inventory.dto.InventoryDeductDTO;
import com.snapshop.inventory.dto.InventoryRecoverDTO;
import com.snapshop.inventory.dto.InventoryResponseDTO;
import com.snapshop.inventory.entity.InventoryLog;
import com.snapshop.inventory.entity.StockRecord;
import com.snapshop.inventory.mapper.InventoryLogMapper;
import com.snapshop.inventory.mapper.SkuStockMapper;
import com.snapshop.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 库存服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final SkuStockMapper skuStockMapper;
    private final InventoryLogMapper inventoryLogMapper;

    /** 库存变更类型：扣减 */
    private static final String CHANGE_TYPE_DEDUCT = "DEDUCT";
    /** 库存变更类型：回补 */
    private static final String CHANGE_TYPE_RECOVER = "RECOVER";

    @Override
    public StockRecord getStock(Long skuId) {
        StockRecord stock = skuStockMapper.selectOne(
                new LambdaQueryWrapper<StockRecord>()
                        .eq(StockRecord::getSkuId, skuId));
        if (stock == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "库存记录不存在: skuId=" + skuId);
        }
        return stock;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryResponseDTO deductStock(InventoryDeductDTO dto) {
        log.info("扣减库存请求: requestId={}, businessKey={}, skuId={}, quantity={}",
                dto.getRequestId(), dto.getBusinessKey(), dto.getSkuId(), dto.getQuantity());

        // 幂等检查：根据 businessKey 查询是否已有流水记录
        if (checkIdempotent(dto.getBusinessKey())) {
            log.info("库存扣减已处理（幂等），businessKey={}", dto.getBusinessKey());
            return InventoryResponseDTO.success();
        }

        // 查询当前库存记录
        StockRecord stock = getStock(dto.getSkuId());
        int beforeStock = stock.getAvailableStock();

        // 执行条件 UPDATE，乐观锁保证不超卖
        int affected = skuStockMapper.deductStock(dto.getSkuId(), dto.getQuantity(), stock.getVersion());
        if (affected == 0) {
            log.warn("库存扣减失败（库存不足或版本冲突）: skuId={}, available={}, quantity={}, version={}",
                    dto.getSkuId(), beforeStock, dto.getQuantity(), stock.getVersion());
            return InventoryResponseDTO.fail("库存扣减失败");
        }

        // 计算扣减后库存
        int afterStock = beforeStock - dto.getQuantity();

        // 写入库存流水
        saveInventoryLog(dto.getBusinessKey(), dto.getSkuId(), CHANGE_TYPE_DEDUCT,
                dto.getQuantity(), beforeStock, afterStock, dto.getScene());

        log.info("库存扣减成功: skuId={}, before={}, after={}, quantity={}",
                dto.getSkuId(), beforeStock, afterStock, dto.getQuantity());
        return InventoryResponseDTO.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryResponseDTO recoverStock(InventoryRecoverDTO dto) {
        log.info("回补库存请求: requestId={}, businessKey={}, skuId={}, quantity={}",
                dto.getRequestId(), dto.getBusinessKey(), dto.getSkuId(), dto.getQuantity());

        // 幂等检查
        if (checkIdempotent(dto.getBusinessKey())) {
            log.info("库存回补已处理（幂等），businessKey={}", dto.getBusinessKey());
            return InventoryResponseDTO.success();
        }

        // 查询当前库存记录
        StockRecord stock = getStock(dto.getSkuId());
        int beforeStock = stock.getAvailableStock();

        // 执行条件 UPDATE，回补可用库存
        int affected = skuStockMapper.recoverStock(dto.getSkuId(), dto.getQuantity(), stock.getVersion());
        if (affected == 0) {
            log.warn("库存回补失败（锁定库存不足或版本冲突）: skuId={}, locked={}, quantity={}, version={}",
                    dto.getSkuId(), stock.getLockedStock(), dto.getQuantity(), stock.getVersion());
            return InventoryResponseDTO.fail("库存回补失败");
        }

        // 计算回补后库存
        int afterStock = beforeStock + dto.getQuantity();

        // 写入库存流水
        saveInventoryLog(dto.getBusinessKey(), dto.getSkuId(), CHANGE_TYPE_RECOVER,
                dto.getQuantity(), beforeStock, afterStock, dto.getReason());

        log.info("库存回补成功: skuId={}, before={}, after={}, quantity={}",
                dto.getSkuId(), beforeStock, afterStock, dto.getQuantity());
        return InventoryResponseDTO.success();
    }

    /**
     * 幂等检查：根据 businessKey 查询是否已存在流水记录
     */
    private boolean checkIdempotent(String businessKey) {
        Long count = inventoryLogMapper.selectCount(
                new LambdaQueryWrapper<InventoryLog>()
                        .eq(InventoryLog::getBusinessKey, businessKey));
        return count != null && count > 0;
    }

    /**
     * 保存库存流水记录
     */
    private void saveInventoryLog(String businessKey, Long skuId, String changeType,
                                  Integer quantity, Integer beforeStock, Integer afterStock,
                                  String reason) {
        InventoryLog log = new InventoryLog();
        log.setBusinessKey(businessKey);
        log.setSkuId(skuId);
        log.setChangeType(changeType);
        log.setQuantity(quantity);
        log.setBeforeAvailableStock(beforeStock);
        log.setAfterAvailableStock(afterStock);
        log.setReason(reason);
        log.setCreatedAt(LocalDateTime.now());
        inventoryLogMapper.insert(log);
    }
}
