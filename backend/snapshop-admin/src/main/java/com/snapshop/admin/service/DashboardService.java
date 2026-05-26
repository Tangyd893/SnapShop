package com.snapshop.admin.service;

import com.snapshop.admin.vo.DashboardStatsVO;

/**
 * 管理后台 Dashboard 服务接口
 */
public interface DashboardService {

    /**
     * 查询 Dashboard 统计数据
     */
    DashboardStatsVO getStats();
}
