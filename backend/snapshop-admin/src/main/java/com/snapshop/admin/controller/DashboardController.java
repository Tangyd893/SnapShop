package com.snapshop.admin.controller;

import com.snapshop.admin.service.DashboardService;
import com.snapshop.admin.vo.DashboardStatsVO;
import com.snapshop.common.base.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台 Dashboard 控制器（T-1305）
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    /**
     * 获取 Dashboard 统计数据
     */
    @GetMapping("/stats")
    public R<DashboardStatsVO> getStats() {
        return R.ok(dashboardService.getStats());
    }
}
