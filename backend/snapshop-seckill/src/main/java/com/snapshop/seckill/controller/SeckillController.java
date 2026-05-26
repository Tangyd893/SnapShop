package com.snapshop.seckill.controller;

import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.PageResult;
import com.snapshop.common.base.R;
import com.snapshop.seckill.service.SeckillService;
import com.snapshop.seckill.vo.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 秒杀控制器
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private SeckillService seckillService;

    /**
     * 查询秒杀活动列表
     */
    @GetMapping("/activities")
    public R<PageResult<SeckillActivityListVO>> getActivityList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PageResult<SeckillActivityListVO> result = seckillService.getActivityList(status, pageNo, pageSize);
        return R.ok(result);
    }

    /**
     * 查询秒杀活动详情
     */
    @GetMapping("/activities/{activityId}")
    public R<SeckillActivityDetailVO> getActivityDetail(@PathVariable Long activityId) {
        SeckillActivityDetailVO vo = seckillService.getActivityDetail(activityId);
        return R.ok(vo);
    }

    /**
     * 查询秒杀商品详情
     */
    @GetMapping("/activities/{activityId}/items/{skuId}")
    public R<SeckillItemDetailVO> getSeckillItemDetail(
            @PathVariable Long activityId,
            @PathVariable Long skuId) {
        String serverTime = LocalDateTime.now().format(FORMATTER);
        SeckillItemDetailVO vo = seckillService.getSeckillItemDetail(activityId, skuId, serverTime);
        return R.ok(vo);
    }

    /**
     * 获取秒杀令牌
     */
    @PostMapping("/activities/{activityId}/items/{skuId}/token")
    public R<SeckillTokenVO> generateToken(
            @PathVariable Long activityId,
            @PathVariable Long skuId,
            HttpServletRequest request) {
        Long userId = getUserIdFromHeader(request);
        SeckillTokenVO vo = seckillService.generateToken(userId, activityId, skuId);
        return R.ok(vo);
    }

    /**
     * 提交秒杀请求
     */
    @PostMapping("/activities/{activityId}/items/{skuId}/submit")
    public R<SeckillResultVO> submitSeckill(
            @PathVariable Long activityId,
            @PathVariable Long skuId,
            @RequestHeader("X-Request-Id") String requestId,
            @RequestBody SeckillSubmitRequest submitRequest,
            HttpServletRequest request) {
        Long userId = getUserIdFromHeader(request);
        SeckillResultVO vo = seckillService.submitSeckill(userId, activityId, skuId,
                submitRequest.getSeckillToken(), requestId,
                submitRequest.getQuantity() != null ? submitRequest.getQuantity() : 1);
        return R.fail(ErrorCode.SECKILL_QUEUING.getCode(), ErrorCode.SECKILL_QUEUING.getMessage(), vo);
    }

    /**
     * 查询秒杀结果
     */
    @GetMapping("/results/{requestId}")
    public R<SeckillResultVO> getResult(@PathVariable String requestId) {
        SeckillResultVO vo = seckillService.getSeckillResult(requestId);
        return R.ok(vo);
    }

    /**
     * 从请求头获取用户编号
     */
    private Long getUserIdFromHeader(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
