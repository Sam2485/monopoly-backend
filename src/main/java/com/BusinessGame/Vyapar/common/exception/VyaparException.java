package com.BusinessGame.Vyapar.common.exception;

import org.springframework.http.HttpStatus;

public class VyaparException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public VyaparException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
