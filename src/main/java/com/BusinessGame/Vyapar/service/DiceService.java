package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.dto.DiceResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Random;

@Service
public class DiceService {

    private final Random random = new SecureRandom();

    public DiceResponse rollDice() {
        int diceOne = random.nextInt(6) + 1;
        int diceTwo = random.nextInt(6) + 1;
        return new DiceResponse(diceOne, diceTwo, diceOne + diceTwo, diceOne == diceTwo);
    }
}
