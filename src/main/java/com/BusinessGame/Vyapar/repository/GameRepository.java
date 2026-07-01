package com.BusinessGame.Vyapar.repository;

import com.BusinessGame.Vyapar.common.enums.GameStatus;
import com.BusinessGame.Vyapar.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    Optional<Game> findByRoomId(UUID roomId);
    List<Game> findByStatus(GameStatus status);
    boolean existsByRoomId(UUID roomId);
}
