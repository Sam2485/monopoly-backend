package com.BusinessGame.Vyapar.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "owned_property")
@IdClass(OwnedPropertyId.class)
public class OwnedProperty {
    @Id
    @Column(name = "game_id")
    private UUID gameId;

    @Id
    @Column(name = "property_id")
    private Integer propertyId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "development_level", nullable = false)
    private Integer developmentLevel; // 0: empty, 1: 1 house, 2: 2 houses, 3: 3 houses, 4: hotel

    @Column(nullable = false)
    private Boolean mortgaged;

    @Version
    private Long version;
}
