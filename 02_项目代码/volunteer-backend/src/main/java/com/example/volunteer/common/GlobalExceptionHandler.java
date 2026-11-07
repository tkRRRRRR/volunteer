package com.example.volunteer.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将业务异常、参数校验异常和未知异常统一转换为 ApiResponse。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidationException(Exception exception) {
        String message = "请求参数不正确";
        if (exception instanceof MethodArgumentNotValidException validationException
                && validationException.getBindingResult().getFieldError() != null) {
            message = validationException.getBindingResult().getFieldError().getDefaultMessage();
        } else if (exception instanceof BindException bindException
                && bindException.getBindingResult().getFieldError() != null) {
            message = bindException.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ApiResponse.failure(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException exception) {
        return ApiResponse.failure(400, exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleUnreadableMessage() {
        return ApiResponse.failure(400, "请求内容格式不正确");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpectedException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ApiResponse.failure(500, "服务器内部异常，请稍后重试");
    }
}
