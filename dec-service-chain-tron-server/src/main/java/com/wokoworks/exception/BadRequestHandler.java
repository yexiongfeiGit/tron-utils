package com.wokoworks.exception;

import com.wokoworks.metrics.MetricsMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Optional;

@Slf4j
@ControllerAdvice
public class BadRequestHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return handleBindingResult(ex.getBindingResult(), status)
                .orElseGet(() -> super.handleMethodArgumentNotValid(ex, headers, status, request));
    }

    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        return handleBindingResult(ex.getBindingResult(), status)
                .orElseGet(() -> super.handleBindException(ex, headers, status, request));
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleException(Throwable ex, WebRequest request) {
        String requestURI;
        if (request instanceof ServletWebRequest) {
            final ServletWebRequest servletwebRequest = (ServletWebRequest) request;
            requestURI = servletwebRequest.getRequest().getRequestURI();
        } else {
            requestURI = "";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(requestURI);
        sb.append(" server internal error:");
        sb.append(ex.getMessage());
        final String errorMsg = sb.toString();
        log.warn(errorMsg, ex);

        MetricsMonitor.exceptionCounter(requestURI, ex.getMessage());

        return ResponseEntity.status(500).body(errorMsg);
    }

    private Optional<ResponseEntity<Object>> handleBindingResult(BindingResult bindingResult, HttpStatus status) {
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            assert fieldError != null;

            StringBuilder sb = new StringBuilder();
            sb.append("输入参数异常 field:");
            sb.append(fieldError.getField());
            sb.append(", message:");
            sb.append(fieldError.getDefaultMessage());
            final String str = sb.toString();

            log.warn(str);
            return Optional.of(ResponseEntity.status(status).body(str));

        }
        return Optional.empty();
    }
}
