package com.gooday.common;

import java.util.Map;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/** 모든 에러를 {"error": "메시지"} 형태로 통일 → 프론트에서 그대로 표시 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        var fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "입력값을 확인해 주세요." : fe.getField() + ": " + fe.getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "요청 형식이 올바르지 않아요."));
    }

    @ExceptionHandler({HandlerMethodValidationException.class, jakarta.validation.ConstraintViolationException.class})
    public ResponseEntity<Map<String, String>> paramInvalid(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "요청 값이 올바르지 않아요. (월은 2027-04 형식)"));
    }
}
