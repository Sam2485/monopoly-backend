package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.enums.GameStatus;
import com.BusinessGame.Vyapar.common.enums.PendingAction;
import com.BusinessGame.Vyapar.common.enums.PlayerStatus;
import com.BusinessGame.Vyapar.common.enums.TransactionType;
import com.BusinessGame.Vyapar.entity.Game;
import com.BusinessGame.Vyapar.entity.Player;
import com.BusinessGame.Vyapar.repository.GameRepository;
import com.BusinessGame.Vyapar.repository.OwnedPropertyRepository;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FinancialService {

    private final PlayerRepository playerRepository;
    private final OwnedPropertyRepository ownedPropertyRepository;
    private final GameRepository gameRepository;
    private final TransactionService transactionService;

    public FinancialService(PlayerRepository playerRepository,
                            OwnedPropertyRepository ownedPropertyRepository,
                            GameRepository gameRepository,
                            TransactionService transactionService) {
        this.playerRepository = playerRepository;
        this.ownedPropertyRepository = ownedPropertyRepository;
        this.gameRepository = gameRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public void enterRecovery(Game game, Player player) {
        player.setStatus(PlayerStatus.RECOVERY);
        playerRepository.save(player);
        game.setPendingAction(PendingAction.RECOVERY);
        gameRepository.save(game);
        transactionService.recordTransaction(
                game.getId(),
                player.getId(),
                TransactionType.BANKRUPTCY,
                0,
                player.getUsername() + " entered Recovery Mode due to negative balance (₹" + player.getBalance() + ")"
        );
    }

    @Transactional
    public boolean checkAndExitRecovery(Game game, Player player) {
        if (player.getBalance() >= 0) {
            player.setStatus(PlayerStatus.ACTIVE);
            playerRepository.save(player);
            game.setPendingAction(PendingAction.NONE);
            gameRepository.save(game);
            transactionService.recordTransaction(
                    game.getId(),
                    player.getId(),
                    TransactionType.BANKRUPTCY,
                    0,
                    player.getUsername() + " successfully recovered. Current balance: ₹" + player.getBalance()
            );
            return true;
        }
        return false;
    }

    @Transactional
    public void declareBankruptcy(Game game, Player player) {
        // Mark player bankrupt
        player.setStatus(PlayerStatus.BANKRUPT);
        player.setBalance(0);
        playerRepository.save(player);

        // Return properties to bank (delete owned property records)
        var ownedProps = ownedPropertyRepository.findByGameIdAndOwnerId(game.getId(), player.getId());
        ownedPropertyRepository.deleteAll(ownedProps);

        // Record transaction
        transactionService.recordTransaction(
                game.getId(),
                player.getId(),
                TransactionType.BANKRUPTCY,
                0,
                player.getUsername() + " has been declared bankrupt!"
        );

        // Check if there is a winner
        checkWinner(game);

        // Reset game pending action to NONE if game is not over
        if (game.getStatus() == GameStatus.STARTED) {
            game.setPendingAction(PendingAction.NONE);
            gameRepository.save(game);
        }
    }

    @Transactional
    public void checkWinner(Game game) {
        List<Player> players = playerRepository.findByGameId(game.getId());
        List<Player> activePlayers = players.stream()
                .filter(p -> p.getStatus() != PlayerStatus.BANKRUPT)
                .collect(Collectors.toList());

        if (activePlayers.size() == 1) {
            Player winner = activePlayers.get(0);
            game.setStatus(GameStatus.FINISHED);
            game.setWinnerId(winner.getId());
            game.setPendingAction(PendingAction.GAME_OVER);
            game.setFinishedAt(LocalDateTime.now());
            gameRepository.save(game);

            transactionService.recordTransaction(
                    game.getId(),
                    winner.getId(),
                    TransactionType.BANKRUPTCY,
                    0,
                    winner.getUsername() + " has won the game!"
            );
        }
    }
}
