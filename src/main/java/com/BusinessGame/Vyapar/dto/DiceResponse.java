package com.BusinessGame.Vyapar.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiceResponse {
    private int diceOne;
    private int diceTwo;
    private int total;
    private boolean isDouble;
}
