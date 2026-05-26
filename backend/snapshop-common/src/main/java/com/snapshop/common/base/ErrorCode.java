package com.snapshop.common.base;

import lombok.Getter;

/**
 * 统一错误码枚举
 */
@Getter
public enum ErrorCode {

    // ========== 通用错误码 ==========
    SUCCESS(0, "成功"),
    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未登录或令牌无效"),
    FORBIDDEN(40300, "无权限"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "请求冲突或重复提交"),
    RATE_LIMITED(42900, "请求过于频繁"),
    INTERNAL_ERROR(50000, "系统异常"),
    REMOTE_CALL_FAILED(50010, "远程服务调用失败"),
    MESSAGE_SEND_FAILED(50020, "消息发送失败"),

    // ========== 认证相关 ==========
    USERNAME_EXISTS(40101, "用户名已存在"),
    PHONE_EXISTS(40102, "手机号已注册"),
    BAD_CREDENTIALS(40103, "用户名或密码错误"),
    TOKEN_EXPIRED(40104, "令牌已过期"),
    TOKEN_INVALID(40105, "令牌无效"),
    USER_DISABLED(40106, "用户已被禁用"),

    // ========== 秒杀业务 ==========
    SECKILL_NOT_STARTED(60000, "活动未开始"),
    SECKILL_ENDED(60001, "活动已结束"),
    SOLD_OUT(60002, "商品已售罄"),
    ALREADY_PARTICIPATED(60003, "用户已参与"),
    NOT_ELIGIBLE(60004, "用户不满足参与资格"),
    SECKILL_QUEUING(60005, "秒杀请求已提交，正在排队处理"),
    SECKILL_SUCCESS(60006, "秒杀订单创建成功"),
    SECKILL_FAILED(60007, "秒杀订单创建失败"),
    SECKILL_RESULT_EXPIRED(60008, "秒杀结果不存在或已过期"),
    SECKILL_TOKEN_INVALID(60009, "秒杀令牌无效或已过期"),

    // ========== 订单相关 ==========
    ORDER_NOT_FOUND(50100, "订单不存在"),
    ORDER_CANNOT_CANCEL(50101, "订单当前状态不允许取消"),
    ORDER_ALREADY_PAID(50102, "订单已支付"),

    // ========== 库存相关 ==========
    STOCK_INSUFFICIENT(50200, "库存不足"),
    STOCK_DEDUCT_FAILED(50201, "库存扣减失败"),

    // ========== 支付相关 ==========
    PAYMENT_NOT_FOUND(50300, "支付单不存在"),
    PAYMENT_ALREADY_PAID(50301, "支付单已支付"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
