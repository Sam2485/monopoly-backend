package com.BusinessGame.Vyapar.config.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardConfig {
    private Integer id;
    private String title;
    private String action; // "ADD_MONEY", "PAY_MONEY", "GO_TO_JAIL", "GET_OUT_OF_JAIL", etc.
    private Integer amount;
}
