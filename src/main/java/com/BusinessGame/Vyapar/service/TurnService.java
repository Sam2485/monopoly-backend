package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.enums.PlayerStatus;
import com.BusinessGame.Vyapar.entity.Game;
import com.BusinessGame.Vyapar.entity.Player;
import com.BusinessGame.Vyapar.repository.GameRepository;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TurnService {

    private final PlayerRepository playerRepository;

    public TurnService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public Player determineNextPlayer(Game game) {
        List<Player> players = playerRepository.findByGameId(game.getId());
        players.sort(java.util.Comparator.comparing(Player::getId));
        
        // Filter out bankrupt players
        List<Player> activePlayers = players.stream()
                .filter(p -> p.getStatus() != PlayerStatus.BANKRUPT)
                .collect(Collectors.toList());

        if (activePlayers.isEmpty()) {
            return null;
        }

        if (game.getCurrentTurnPlayerId() == null) {
            // Start with the first player
            return activePlayers.get(0);
        }

        // Find index of current player
        int currentIndex = -1;
        for (int i = 0; i < activePlayers.size(); i++) {
            if (activePlayers.get(i).getId().equals(game.getCurrentTurnPlayerId())) {
                currentIndex = i;
                break;
            }
        }

        // Cycle through players to find the next one who doesn't have skipped turns
        int nextIndex = currentIndex;
        for (int i = 0; i < activePlayers.size(); i++) {
            nextIndex = (nextIndex + 1) % activePlayers.size();
            Player nextPlayer = activePlayers.get(nextIndex);
            
            if (nextPlayer.getSkippedTurns() > 0) {
                // Decrement skipped turns and save
                nextPlayer.setSkippedTurns(nextPlayer.getSkippedTurns() - 1);
                playerRepository.save(nextPlayer);
                // Continue loop to check next player
            } else {
                return nextPlayer;
            }
        }

        // If everyone has skipped turns, return the first one available
        return activePlayers.get((currentIndex + 1) % activePlayers.size());
    }
}
