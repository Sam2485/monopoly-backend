package com.BusinessGame.Vyapar.common.exception;

import org.springframework.http.HttpStatus;

public class PropertyNotFoundException extends VyaparException {
    public PropertyNotFoundException(String message) {
        super(message, "PROPERTY_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
