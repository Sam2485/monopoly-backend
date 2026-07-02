package com.BusinessGame.Vyapar.controller;

import com.BusinessGame.Vyapar.dto.ActionRequest;
import com.BusinessGame.Vyapar.dto.ApiResponse;
import com.BusinessGame.Vyapar.dto.GameStateResponse;
import com.BusinessGame.Vyapar.dto.TransactionResponse;
import com.BusinessGame.Vyapar.entity.User;
import com.BusinessGame.Vyapar.service.AuthenticationService;
import com.BusinessGame.Vyapar.service.GameEngineFacade;
import com.BusinessGame.Vyapar.service.GameService;
import com.BusinessGame.Vyapar.service.TransactionService;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameService gameService;
    private final GameEngineFacade gameEngineFacade;
    private final AuthenticationService authenticationService;
    private final PlayerRepository playerRepository;
    private final TransactionService transactionService;

    public GameController(GameService gameService,
                          GameEngineFacade gameEngineFacade,
                          AuthenticationService authenticationService,
                          PlayerRepository playerRepository,
                          TransactionService transactionService) {
        this.gameService = gameService;
        this.gameEngineFacade = gameEngineFacade;
        this.authenticationService = authenticationService;
        this.playerRepository = playerRepository;
        this.transactionService = transactionService;
    }

    @GetMapping("/{gameId}")
    public ApiResponse<GameStateResponse> getGameState(@PathVariable UUID gameId) {
        GameStateResponse response = gameService.getGameState(gameId);
        return ApiResponse.success(response, "Loaded game state");
    }

    @PostMapping("/{gameId}/actions")
    public ApiResponse<GameStateResponse> performAction(
            @PathVariable UUID gameId,
            @RequestBody ActionRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        // Resolve the Player ID from Game ID and User ID
        com.BusinessGame.Vyapar.entity.Player player = playerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new com.BusinessGame.Vyapar.common.exception.PlayerNotFoundException(
                        "Player not found for user " + currentUser.getUsername() + " in game " + gameId));

        GameStateResponse state = gameEngineFacade.performAction(gameId, player.getId(), request);
        return ApiResponse.success(state, "Action performed successfully");
    }

    @GetMapping("/{gameId}/transactions")
    public ApiResponse<List<TransactionResponse>> getGameTransactions(@PathVariable UUID gameId) {
        List<TransactionResponse> history = transactionService.getGameHistory(gameId);
        return ApiResponse.success(history, "Loaded game transactions");
    }
}
