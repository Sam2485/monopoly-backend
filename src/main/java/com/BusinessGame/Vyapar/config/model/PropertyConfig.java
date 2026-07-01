package com.BusinessGame.Vyapar.config.model;

import com.BusinessGame.Vyapar.common.enums.TileType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyConfig {
    private Integer id;
    private TileType type;
    private String name;
    private String group;
    private Integer price;
    private Integer housePrice;
    private Integer hotelPrice;
    private List<Integer> rent;
    private String rentType; // "FIXED", "DICE_MULTIPLIER"
    private Integer diceMultiplier;
    private Integer baseRent;
}
