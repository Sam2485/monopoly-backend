package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.enums.PlayerStatus;
import com.BusinessGame.Vyapar.common.enums.TransactionType;
import com.BusinessGame.Vyapar.config.model.CardConfig;
import com.BusinessGame.Vyapar.entity.Player;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

@Service
public class CardService {

    private final JsonLoaderService jsonLoaderService;
    private final PlayerRepository playerRepository;
    private final TransactionService transactionService;
    private final Random random = new SecureRandom();

    public CardService(JsonLoaderService jsonLoaderService,
                       PlayerRepository playerRepository,
                       TransactionService transactionService) {
        this.jsonLoaderService = jsonLoaderService;
        this.playerRepository = playerRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public CardConfig drawChanceCard(Player player, int diceTotal) {
        boolean isEven = (diceTotal % 2 == 0);
        List<CardConfig> deck = isEven ? jsonLoaderService.getChanceRewards() : jsonLoaderService.getChancePunishments();
        CardConfig card = deck.get(random.nextInt(deck.size()));
        executeCardAction(player, card, TransactionType.CHANCE);
        return card;
    }

    @Transactional
    public CardConfig drawCommunityCard(Player player, int diceTotal) {
        boolean isEven = (diceTotal % 2 == 0);
        List<CardConfig> deck = isEven ? jsonLoaderService.getCommunityRewards() : jsonLoaderService.getCommunityPunishments();
        CardConfig card = deck.get(random.nextInt(deck.size()));
        executeCardAction(player, card, TransactionType.COMMUNITY);
        return card;
    }

    private void executeCardAction(Player player, CardConfig card, TransactionType transType) {
        String action = card.getAction();
        int amount = card.getAmount() != null ? card.getAmount() : 0;
        String desc = card.getTitle();

        switch (action) {
            case "ADD_MONEY":
                player.setBalance(player.getBalance() + amount);
                transactionService.recordTransaction(player.getGameId(), player.getId(), transType, amount, "Drawn Card: " + desc + " (+₹" + amount + ")");
                break;
            case "PAY_MONEY":
                player.setBalance(player.getBalance() - amount);
                transactionService.recordTransaction(player.getGameId(), player.getId(), transType, amount, "Drawn Card: " + desc + " (-₹" + amount + ")");
                break;
            case "GO_TO_JAIL":
                player.setPosition(9); // Jail position index
                player.setStatus(PlayerStatus.IN_JAIL);
                player.setJailTurns(0);
                transactionService.recordTransaction(player.getGameId(), player.getId(), transType, 0, "Drawn Card: " + desc + " (Sent to Jail)");
                break;
            case "GET_OUT_OF_JAIL":
                // Crediting bail value ₹500 so they effectively get out of jail free
                player.setBalance(player.getBalance() + 500);
                transactionService.recordTransaction(player.getGameId(), player.getId(), transType, 500, "Drawn Card: " + desc + " (+₹500 bail refund)");
                break;
            case "MOVE_TO_START":
                player.setPosition(0);
                int reward = jsonLoaderService.getGameRules().getPassStartReward();
                player.setBalance(player.getBalance() + reward);
                transactionService.recordTransaction(player.getGameId(), player.getId(), transType, reward, "Drawn Card: " + desc + " (Moved to START, +₹" + reward + ")");
                break;
            case "LOSE_NEXT_TURN":
                player.setSkippedTurns(player.getSkippedTurns() + 1);
                transactionService.recordTransaction(player.getGameId(), player.getId(), transType, 0, "Drawn Card: " + desc + " (Lose next turn)");
                break;
            case "MOVE_BACK":
                int newPos = (player.getPosition() - 3 + 36) % 36; // Default move back 3 spaces if amount not set
                if (amount > 0) {
                    newPos = (player.getPosition() - amount + 36) % 36;
                }
                player.setPosition(newPos);
                transactionService.recordTransaction(player.getGameId(), player.getId(), transType, 0, "Drawn Card: " + desc + " (Moved back to tile " + newPos + ")");
                break;
            default:
                break;
        }

        playerRepository.save(player);
    }
}
