package com.BusinessGame.Vyapar.common.exception;

import org.springframework.http.HttpStatus;

public class GameAlreadyStartedException extends VyaparException {
    public GameAlreadyStartedException(String message) {
        super(message, "GAME_ALREADY_STARTED", HttpStatus.CONFLICT);
    }
}
