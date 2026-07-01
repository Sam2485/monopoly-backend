package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.enums.TransactionType;
import com.BusinessGame.Vyapar.dto.TransactionResponse;
import com.BusinessGame.Vyapar.entity.Transaction;
import com.BusinessGame.Vyapar.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction recordTransaction(UUID gameId, UUID playerId, TransactionType type, Integer amount, String description) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setGameId(gameId);
        transaction.setPlayerId(playerId);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setCreatedAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getGameHistory(UUID gameId) {
        return transactionRepository.findByGameIdOrderByCreatedAtAsc(gameId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getPlayerHistory(UUID playerId) {
        return transactionRepository.findByPlayerId(playerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
