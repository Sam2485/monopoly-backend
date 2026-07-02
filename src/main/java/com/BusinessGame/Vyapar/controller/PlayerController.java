package com.BusinessGame.Vyapar.controller;

import com.BusinessGame.Vyapar.common.exception.PlayerNotFoundException;
import com.BusinessGame.Vyapar.dto.ApiResponse;
import com.BusinessGame.Vyapar.dto.PlayerResponse;
import com.BusinessGame.Vyapar.dto.TransactionResponse;
import com.BusinessGame.Vyapar.entity.Player;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import com.BusinessGame.Vyapar.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final TransactionService transactionService;

    public PlayerController(PlayerRepository playerRepository, TransactionService transactionService) {
        this.playerRepository = playerRepository;
        this.transactionService = transactionService;
    }

    @GetMapping("/{playerId}")
    public ApiResponse<PlayerResponse> getPlayerDetails(@PathVariable UUID playerId) {
        Player p = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with ID: " + playerId));
        PlayerResponse response = new PlayerResponse(
                p.getId(),
                p.getUsername(),
                p.getBalance(),
                p.getPosition(),
                p.getNumberOfProperties(),
                p.getStatus(),
                p.getConnected(),
                p.getHasBuiltHouseThisTurn(),
                p.getTokenColor() != null ? p.getTokenColor() : "#a855f7"
        );
        return ApiResponse.success(response, "Loaded player details");
    }

    @GetMapping("/{playerId}/transactions")
    public ApiResponse<List<TransactionResponse>> getPlayerTransactions(@PathVariable UUID playerId) {
        List<TransactionResponse> history = transactionService.getPlayerHistory(playerId);
        return ApiResponse.success(history, "Loaded player transaction history");
    }
}
