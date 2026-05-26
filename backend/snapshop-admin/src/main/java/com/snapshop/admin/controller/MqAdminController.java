package com.snapshop.admin.controller;

import com.snapshop.admin.annotation.RequireRole;
import com.snapshop.admin.entity.LocalMessage;
import com.snapshop.admin.service.MqAdminService;
import com.snapshop.common.base.PageResult;
import com.snapshop.common.base.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台 MQ 死信运维控制器（T-1322）
 */
@RestController
@RequestMapping("/api/admin/mq")
public class MqAdminController {

    @Resource
    private MqAdminService mqAdminService;

    /**
     * 死信消息分页查询
     */
    @GetMapping("/dead-letters")
    public R<PageResult<LocalMessage>> getDeadLetters(
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PageResult<LocalMessage> result = mqAdminService.getDeadLetters(pageNo, pageSize);
        return R.ok(result);
    }

    /**
     * 死信消息重投
     */
    @PostMapping("/dead-letters/{id}/requeue")
    @RequireRole("SUPER_ADMIN")
    public R<Void> requeueDeadLetter(@PathVariable Long id) {
        mqAdminService.requeueDeadLetter(id);
        return R.ok();
    }
}
