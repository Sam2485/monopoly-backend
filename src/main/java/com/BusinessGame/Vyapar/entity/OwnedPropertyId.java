package com.BusinessGame.Vyapar.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnedPropertyId implements Serializable {
    private UUID gameId;
    private Integer propertyId;
}
