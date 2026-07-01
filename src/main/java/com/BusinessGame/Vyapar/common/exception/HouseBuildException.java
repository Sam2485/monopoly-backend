package com.BusinessGame.Vyapar.common.exception;

import org.springframework.http.HttpStatus;

public class HouseBuildException extends VyaparException {
    public HouseBuildException(String message) {
        super(message, "HOUSE_LIMIT_REACHED", HttpStatus.BAD_REQUEST);
    }
}
