package com.snapshop.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.seckill.entity.SeckillActivityItem;
import com.snapshop.seckill.vo.SeckillItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 秒杀活动商品 Mapper
 */
@Mapper
public interface SeckillActivityItemMapper extends BaseMapper<SeckillActivityItem> {

    /**
     * 查询活动商品列表，关联 product 和 sku 表获取商品标题和原价
     *
     * @param activityId 活动编号
     * @return 活动商品 VO 列表
     */
    List<SeckillItemVO> selectItemsWithProduct(@Param("activityId") Long activityId);

    /**
     * 查询单个活动商品详情，关联 product 和 sku 表
     *
     * @param activityId 活动编号
     * @param skuId      商品规格编号
     * @return 活动商品 VO
     */
    SeckillItemVO selectItemWithProduct(@Param("activityId") Long activityId, @Param("skuId") Long skuId);
}
