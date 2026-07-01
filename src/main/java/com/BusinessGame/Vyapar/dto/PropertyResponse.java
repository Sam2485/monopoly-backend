package com.BusinessGame.Vyapar.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponse {
    private Integer propertyId;
    private String propertyName;
    private String group;
    private UUID ownerId;
    private Integer developmentLevel;
    private boolean mortgaged;
}
