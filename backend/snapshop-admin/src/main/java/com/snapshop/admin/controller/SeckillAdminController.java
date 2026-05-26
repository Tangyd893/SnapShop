package com.snapshop.admin.controller;

import com.snapshop.admin.annotation.RequireRole;
import com.snapshop.admin.entity.SeckillActivity;
import com.snapshop.admin.entity.SeckillActivityItem;
import com.snapshop.admin.service.SeckillAdminService;
import com.snapshop.common.base.PageResult;
import com.snapshop.common.base.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台秒杀活动控制器（T-1316 / T-1317 / T-1318）
 */
@RestController
@RequestMapping("/api/admin/seckill")
public class SeckillAdminController {

    @Resource
    private SeckillAdminService seckillAdminService;

    /**
     * 秒杀活动列表
     */
    @GetMapping("/activities")
    public R<PageResult<SeckillActivity>> getActivityList(
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PageResult<SeckillActivity> result = seckillAdminService.getActivityList(pageNo, pageSize);
        return R.ok(result);
    }

    /**
     * 创建秒杀活动
     */
    @PostMapping("/activities")
    public R<SeckillActivity> createActivity(@RequestBody SeckillActivity activity) {
        return R.ok(seckillAdminService.createActivity(activity));
    }

    /**
     * 更新秒杀活动
     */
    @PutMapping("/activities/{id}")
    public R<SeckillActivity> updateActivity(@PathVariable Long id, @RequestBody SeckillActivity activity) {
        return R.ok(seckillAdminService.updateActivity(id, activity));
    }

    /**
     * 绑定秒杀商品到活动
     */
    @PostMapping("/activities/{id}/items")
    public R<SeckillActivityItem> addActivityItem(@PathVariable Long id, @RequestBody SeckillActivityItem item) {
        return R.ok(seckillAdminService.addActivityItem(id, item));
    }

    /**
     * 触发 Redis 预热
     */
    @PostMapping("/activities/{id}/warmup")
    @RequireRole("SUPER_ADMIN")
    public R<Void> warmUpStock(@PathVariable Long id) {
        seckillAdminService.warmUpStock(id);
        return R.ok();
    }
}
