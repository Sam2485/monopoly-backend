package com.BusinessGame.Vyapar.common.exception;

import org.springframework.http.HttpStatus;

public class PropertyAlreadyOwnedException extends VyaparException {
    public PropertyAlreadyOwnedException(String message) {
        super(message, "PROPERTY_OWNED", HttpStatus.CONFLICT);
    }
}
