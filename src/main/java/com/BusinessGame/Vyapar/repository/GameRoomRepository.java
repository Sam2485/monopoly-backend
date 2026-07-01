package com.BusinessGame.Vyapar.repository;

import com.BusinessGame.Vyapar.common.enums.RoomStatus;
import com.BusinessGame.Vyapar.entity.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, UUID> {
    Optional<GameRoom> findByRoomCode(String roomCode);
    boolean existsByRoomCode(String roomCode);
    List<GameRoom> findByStatus(RoomStatus status);
}
