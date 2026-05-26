package com.snapshop.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存操作响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponseDTO {

    /** 是否成功 */
    private boolean success;

    /** 失败原因 */
    private String failureReason;

    public static InventoryResponseDTO success() {
        return new InventoryResponseDTO(true, null);
    }

    public static InventoryResponseDTO fail(String reason) {
        return new InventoryResponseDTO(false, reason);
    }
}
