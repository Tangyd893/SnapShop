package com.snapshop.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.inventory.entity.StockRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存 Mapper
 */
@Mapper
public interface SkuStockMapper extends BaseMapper<StockRecord> {

    /**
     * 条件更新：扣减可用库存，增加锁定库存（乐观锁）
     *
     * @param skuId     商品规格编号
     * @param quantity  扣减数量
     * @param version   当前版本号
     * @return 影响行数，0 表示扣减失败
     */
    @Update("UPDATE sku_stock SET available_stock = available_stock - #{quantity}, " +
            "locked_stock = locked_stock + #{quantity}, version = version + 1 " +
            "WHERE sku_id = #{skuId} AND available_stock >= #{quantity} AND version = #{version}")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity,
                    @Param("version") Integer version);

    /**
     * 条件更新：回补可用库存，减少锁定库存（乐观锁）
     *
     * @param skuId     商品规格编号
     * @param quantity  回补数量
     * @param version   当前版本号
     * @return 影响行数，0 表示回补失败
     */
    @Update("UPDATE sku_stock SET available_stock = available_stock + #{quantity}, " +
            "locked_stock = locked_stock - #{quantity}, version = version + 1 " +
            "WHERE sku_id = #{skuId} AND locked_stock >= #{quantity} AND version = #{version}")
    int recoverStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity,
                     @Param("version") Integer version);
}
