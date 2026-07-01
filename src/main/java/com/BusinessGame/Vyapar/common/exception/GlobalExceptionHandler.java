package com.BusinessGame.Vyapar.common.exception;

import com.BusinessGame.Vyapar.dto.ApiResponse;
import com.BusinessGame.Vyapar.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VyaparException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleVyaparException(VyaparException ex) {
        log.warn("Business rule validation failed: {} (Code: {})", ex.getMessage(), ex.getErrorCode());
        ErrorResponse error = new ErrorResponse(ex.getErrorCode(), ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(false, ex.getMessage(), error, java.time.LocalDateTime.now());
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGenericException(Exception ex) {
        log.error("Internal server error occurred", ex);
        ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred: " + ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(false, "Internal Server Error", error, java.time.LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
