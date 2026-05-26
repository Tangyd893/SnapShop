package com.snapshop.common.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应结果
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private int code;
    private String message;
    private T data;
    private String requestId;
    private String timestamp;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private R() {
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 0;
        r.message = "成功";
        r.data = data;
        r.timestamp = LocalDateTime.now().format(FORMATTER);
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        r.timestamp = LocalDateTime.now().format(FORMATTER);
        return r;
    }

    public static <T> R<T> fail(int code, String message, T data) {
        R<T> r = fail(code, message);
        r.data = data;
        return r;
    }

    public R<T> requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
