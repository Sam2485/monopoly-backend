package com.BusinessGame.Vyapar.repository;

import com.BusinessGame.Vyapar.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByGameId(UUID gameId);
    List<Transaction> findByPlayerId(UUID playerId);
    List<Transaction> findByGameIdOrderByCreatedAtAsc(UUID gameId);
}
