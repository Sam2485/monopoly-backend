package com.BusinessGame.Vyapar.common.exception;

import org.springframework.http.HttpStatus;

public class GameNotFoundException extends VyaparException {
    public GameNotFoundException(String message) {
        super(message, "GAME_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
