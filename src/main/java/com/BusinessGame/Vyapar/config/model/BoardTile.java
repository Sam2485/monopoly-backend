package com.BusinessGame.Vyapar.config.model;

import com.BusinessGame.Vyapar.common.enums.TileType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardTile {
    private Integer position;
    private TileType type;
    private String name;
    private Integer propertyId;
}
