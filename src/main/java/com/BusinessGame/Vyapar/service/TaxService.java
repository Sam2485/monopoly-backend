package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.common.enums.TransactionType;
import com.BusinessGame.Vyapar.entity.Player;
import com.BusinessGame.Vyapar.repository.OwnedPropertyRepository;
import com.BusinessGame.Vyapar.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaxService {

    private final PlayerRepository playerRepository;
    private final OwnedPropertyRepository ownedPropertyRepository;
    private final TransactionService transactionService;

    public TaxService(PlayerRepository playerRepository,
                      OwnedPropertyRepository ownedPropertyRepository,
                      TransactionService transactionService) {
        this.playerRepository = playerRepository;
        this.ownedPropertyRepository = ownedPropertyRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public int calculateIncomeTax(Player player) {
        // 2% of current balance (rounded down)
        int balance = player.getBalance();
        if (balance <= 0) return 0;
        return (int) (balance * 0.02);
    }

    @Transactional
    public int calculateWealthTax(Player player) {
        // 50 * Number of Owned Properties
        long ownedCount = ownedPropertyRepository.countByGameIdAndOwnerId(player.getGameId(), player.getId());
        return (int) (50 * ownedCount);
    }

    @Transactional
    public void payIncomeTax(Player player) {
        int taxAmount = calculateIncomeTax(player);
        player.setBalance(player.getBalance() - taxAmount);
        playerRepository.save(player);
        transactionService.recordTransaction(
                player.getGameId(),
                player.getId(),
                TransactionType.INCOME_TAX,
                taxAmount,
                "Paid Income Tax (2% of balance): ₹" + taxAmount
        );
    }

    @Transactional
    public void payWealthTax(Player player) {
        int taxAmount = calculateWealthTax(player);
        player.setBalance(player.getBalance() - taxAmount);
        playerRepository.save(player);
        transactionService.recordTransaction(
                player.getGameId(),
                player.getId(),
                TransactionType.WEALTH_TAX,
                taxAmount,
                "Paid Wealth Tax (₹50 per property): ₹" + taxAmount
        );
    }
}
