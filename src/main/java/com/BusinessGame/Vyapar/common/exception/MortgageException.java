package com.BusinessGame.Vyapar.common.exception;

import org.springframework.http.HttpStatus;

public class MortgageException extends VyaparException {
    public MortgageException(String message) {
        super(message, "PROPERTY_MORTGAGED", HttpStatus.BAD_REQUEST);
    }
}
