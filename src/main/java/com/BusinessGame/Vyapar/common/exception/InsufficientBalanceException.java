package com.BusinessGame.Vyapar.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends VyaparException {
    public InsufficientBalanceException(String message) {
        super(message, "NOT_ENOUGH_MONEY", HttpStatus.BAD_REQUEST);
    }
}
