package com.BusinessGame.Vyapar.repository;

import com.BusinessGame.Vyapar.entity.OwnedProperty;
import com.BusinessGame.Vyapar.entity.OwnedPropertyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OwnedPropertyRepository extends JpaRepository<OwnedProperty, OwnedPropertyId> {
    Optional<OwnedProperty> findByGameIdAndPropertyId(UUID gameId, Integer propertyId);
    List<OwnedProperty> findByGameId(UUID gameId);
    List<OwnedProperty> findByOwnerId(UUID ownerId);
    List<OwnedProperty> findByGameIdAndOwnerId(UUID gameId, UUID ownerId);
    long countByGameIdAndOwnerId(UUID gameId, UUID ownerId);
}
