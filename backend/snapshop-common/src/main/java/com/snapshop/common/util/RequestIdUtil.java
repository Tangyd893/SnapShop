package com.snapshop.common.util;

import java.util.UUID;

/**
 * 请求编号工具类
 */
public final class RequestIdUtil {

    private RequestIdUtil() {
    }

    public static String generate() {
        return "REQ" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
