package com.BusinessGame.Vyapar.common.exception;

import org.springframework.http.HttpStatus;

public class PlayerNotFoundException extends VyaparException {
    public PlayerNotFoundException(String message) {
        super(message, "PLAYER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
