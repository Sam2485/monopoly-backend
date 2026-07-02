package com.BusinessGame.Vyapar.controller;

import com.BusinessGame.Vyapar.dto.ApiResponse;
import com.BusinessGame.Vyapar.dto.TradeOfferResponse;
import com.BusinessGame.Vyapar.dto.TradeProposalRequest;
import com.BusinessGame.Vyapar.entity.Player;
import com.BusinessGame.Vyapar.entity.User;
import com.BusinessGame.Vyapar.service.AuthenticationService;
import com.BusinessGame.Vyapar.service.TradeService;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import com.BusinessGame.Vyapar.common.exception.PlayerNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/games/{gameId}/trades")
public class TradeController {

    private final TradeService tradeService;
    private final AuthenticationService authenticationService;
    private final PlayerRepository playerRepository;

    public TradeController(TradeService tradeService,
                           AuthenticationService authenticationService,
                           PlayerRepository playerRepository) {
        this.tradeService = tradeService;
        this.authenticationService = authenticationService;
        this.playerRepository = playerRepository;
    }

    @PostMapping
    public ApiResponse<TradeOfferResponse> proposeTrade(
            @PathVariable UUID gameId,
            @RequestBody TradeProposalRequest request) {
        User currentUser = authenticationService.getCurrentUser();
        Player player = playerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new PlayerNotFoundException(
                        "Player not found for user " + currentUser.getUsername() + " in game " + gameId));

        TradeOfferResponse response = tradeService.proposeTrade(gameId, player.getId(), request);
        return ApiResponse.success(response, "Trade proposed successfully");
    }

    @GetMapping("/pending")
    public ApiResponse<List<TradeOfferResponse>> getPendingTrades(@PathVariable UUID gameId) {
        User currentUser = authenticationService.getCurrentUser();
        Player player = playerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new PlayerNotFoundException(
                        "Player not found for user " + currentUser.getUsername() + " in game " + gameId));

        List<TradeOfferResponse> response = tradeService.getPendingTrades(gameId, player.getId());
        return ApiResponse.success(response, "Pending trades fetched successfully");
    }

    @PostMapping("/{tradeId}/accept")
    public ApiResponse<Void> acceptTrade(
            @PathVariable UUID gameId,
            @PathVariable UUID tradeId) {
        User currentUser = authenticationService.getCurrentUser();
        Player player = playerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new PlayerNotFoundException(
                        "Player not found for user " + currentUser.getUsername() + " in game " + gameId));

        tradeService.acceptTrade(gameId, player.getId(), tradeId);
        return ApiResponse.success(null, "Trade accepted successfully");
    }

    @PostMapping("/{tradeId}/reject")
    public ApiResponse<Void> rejectTrade(
            @PathVariable UUID gameId,
            @PathVariable UUID tradeId) {
        User currentUser = authenticationService.getCurrentUser();
        Player player = playerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new PlayerNotFoundException(
                        "Player not found for user " + currentUser.getUsername() + " in game " + gameId));

        tradeService.rejectTrade(gameId, player.getId(), tradeId);
        return ApiResponse.success(null, "Trade rejected successfully");
    }

    @PostMapping("/{tradeId}/cancel")
    public ApiResponse<Void> cancelTrade(
            @PathVariable UUID gameId,
            @PathVariable UUID tradeId) {
        User currentUser = authenticationService.getCurrentUser();
        Player player = playerRepository.findByGameIdAndUserId(gameId, currentUser.getId())
                .orElseThrow(() -> new PlayerNotFoundException(
                        "Player not found for user " + currentUser.getUsername() + " in game " + gameId));

        tradeService.cancelTrade(gameId, player.getId(), tradeId);
        return ApiResponse.success(null, "Trade cancelled successfully");
    }
}
