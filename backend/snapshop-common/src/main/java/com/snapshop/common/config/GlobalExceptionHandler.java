package com.snapshop.common.config;

import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] {}: {}", e.getCode(), request.getRequestURI(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage())
                .requestId(getRequestId(request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), msg)
                .requestId(getRequestId(request));
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBindException(BindException e, HttpServletRequest request) {
        String msg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), msg)
                .requestId(getRequestId(request));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}] {}", request.getRequestURI(), e.getMessage(), e);
        return R.fail(ErrorCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试")
                .requestId(getRequestId(request));
    }

    private String getRequestId(HttpServletRequest request) {
        return request.getHeader("X-Request-Id");
    }
}
