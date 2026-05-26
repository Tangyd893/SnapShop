package com.snapshop.order.feign;

import com.snapshop.common.base.R;
import com.snapshop.order.dto.InventoryDeductDTO;
import com.snapshop.order.dto.InventoryRecoverDTO;
import com.snapshop.order.dto.InventoryResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 库存服务 Feign 客户端
 */
@FeignClient(name = "snapshop-inventory", path = "/internal/inventory")
public interface InventoryFeignClient {

    /**
     * 扣减库存
     *
     * @param dto 库存扣减请求
     * @return 库存扣减结果
     */
    @PostMapping("/deduct")
    R<InventoryResponseDTO> deductStock(@RequestBody InventoryDeductDTO dto);

    /**
     * 回补库存
     *
     * @param dto 库存回补请求
     * @return 库存回补结果
     */
    @PostMapping("/recover")
    R<InventoryResponseDTO> recover(@RequestBody InventoryRecoverDTO dto);
}
