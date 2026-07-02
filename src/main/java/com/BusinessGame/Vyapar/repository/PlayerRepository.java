package com.BusinessGame.Vyapar.repository;

import com.BusinessGame.Vyapar.common.enums.PlayerStatus;
import com.BusinessGame.Vyapar.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findByGameId(UUID gameId);
    Optional<Player> findByGameIdAndUserId(UUID gameId, UUID userId);
    Optional<Player> findByGameIdAndId(UUID gameId, UUID playerId);
    List<Player> findByGameIdOrderByPosition(UUID gameId);
    long countByGameIdAndStatus(UUID gameId, PlayerStatus status);
    List<Player> findByUserId(UUID userId);
}
