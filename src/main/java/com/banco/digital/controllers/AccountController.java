package com.banco.digital.controllers;

import com.banco.digital.models.Account;
import com.banco.digital.models.User;
import com.banco.digital.repositories.AccountRepository;
import com.banco.digital.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final com.banco.digital.repositories.TransactionRepository transactionRepository;

    public AccountController(AccountRepository accountRepository, 
                             UserRepository userRepository, 
                             com.banco.digital.repositories.TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<List<Account>> getMyAccounts(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(accountRepository.findByUserId(user.getId()));
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> request) {
        String accountNumber = (String) request.get("accountNumber");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Cuenta " + accountNumber + " no encontrada en el sistema"));
        
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        
        // Registrar el movimiento en el historial
        com.banco.digital.models.Transaction transaction = com.banco.digital.models.Transaction.builder()
                .toAccountId(account.getId())
                .amount(amount)
                .type("DEPOSIT")
                .description("Abono de Capital (Terminal)")
                .build();
        transactionRepository.save(transaction);
        
        return ResponseEntity.ok("Depósito de " + amount + " realizado con éxito en cuenta " + accountNumber);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(Authentication authentication, @RequestBody Map<String, String> request) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String type = request.getOrDefault("type", "SAVINGS");
        
        // Generar número de cuenta aleatorio de 16 dígitos
        String accountNumber = java.util.stream.IntStream.range(0, 16)
                .mapToObj(i -> String.valueOf((int) (Math.random() * 10)))
                .reduce("", String::concat);

        Account newAccount = Account.builder()
                .type(type)
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .user(user)
                .build();

        return ResponseEntity.ok(accountRepository.save(newAccount));
    }
}
