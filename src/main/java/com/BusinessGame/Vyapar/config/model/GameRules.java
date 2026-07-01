package com.BusinessGame.Vyapar.config.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRules {
    private Integer startingMoney;
    private Integer passStartReward;
    private Integer jailBail;
    private Integer clubFee;
    private Integer incomeTaxPercentage;
    private Integer wealthTaxPerProperty;
    private Integer mortgagePercentage;
    private Integer unmortgageInterest;
    private Integer houseSellPercentage;
    private Integer hotelSellPercentage;
    private Integer majorityOwnership;
    private Integer maximumPlayers;
    private Integer minimumPlayers;
    private Integer maximumHouses;
}
