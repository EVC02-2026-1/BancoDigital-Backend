package com.banco.digital.services;

import com.banco.digital.models.Account;
import com.banco.digital.models.Transaction;
import com.banco.digital.repositories.AccountRepository;
import com.banco.digital.repositories.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void transfer(String fromNum, String toNum, java.math.BigDecimal amount, String description) {
        Account fromAccount = accountRepository.findByAccountNumber(fromNum)
                .orElseThrow(() -> new RuntimeException("Cuenta de origen no encontrada"));
        
        Account toAccount = accountRepository.findByAccountNumber(toNum)
                .orElseThrow(() -> new RuntimeException("Cuenta de destino no encontrada"));

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Saldo insuficiente");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = Transaction.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(amount)
                .type("TRANSFER")
                .description(description)
                .build();
        
        transactionRepository.save(transaction);
    }

    public List<Transaction> getHistory(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        List<Long> ids = accounts.stream().map(Account::getId).collect(Collectors.toList());
        if (ids.isEmpty()) return java.util.Collections.emptyList();
        return transactionRepository.findByAccountIds(ids);
    }
}
