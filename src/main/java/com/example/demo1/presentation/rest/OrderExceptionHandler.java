package com.example.demo1.presentation.rest;

import com.example.demo1.application.service.OrderNotFoundException;
import com.example.demo1.domain.model.InvalidOrderStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderExceptionHandler {
    // Chỉ ánh xạ lỗi nghiệp vụ cụ thể; lỗi hệ thống vẫn được Spring xử lý riêng.
    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    ProblemDetail invalidState(InvalidOrderStateException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
