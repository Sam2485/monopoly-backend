package com.BusinessGame.Vyapar.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidTurnException extends VyaparException {
    public InvalidTurnException(String message) {
        super(message, "INVALID_TURN", HttpStatus.BAD_REQUEST);
    }
}
