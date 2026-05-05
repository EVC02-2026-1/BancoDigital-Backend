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
    private final com.banco.digital.services.AuditService auditService;

    public AccountController(AccountRepository accountRepository, 
                             UserRepository userRepository, 
                             com.banco.digital.repositories.TransactionRepository transactionRepository,
                             com.banco.digital.services.AuditService auditService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
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
        
        auditService.logAction("system", "DEPOSIT", "SUCCESS", "Depósito de " + amount + " en cuenta " + accountNumber, "system");
        
        return ResponseEntity.ok("Depósito de " + amount + " realizado con éxito en cuenta " + accountNumber);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(Authentication authentication, @RequestBody Map<String, String> request) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Regla HU-08: Solo usuarios verificados (KYC)
        if (!user.isVerified()) {
            auditService.logAction(user.getEmail(), "CREATE_ACCOUNT_FAILED", "FAILED", "Intento de crear cuenta sin verificación KYC", "system");
            return ResponseEntity.status(403).body("Debes completar la verificación de identidad (KYC) antes de crear cuentas adicionales.");
        }

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

        Account saved = accountRepository.save(newAccount);
        auditService.logAction(user.getEmail(), "CREATE_ACCOUNT_SUCCESS", "SUCCESS", "Nueva cuenta creada: " + accountNumber + " (" + type + ")", "system");
        
        return ResponseEntity.ok(saved);
    }
}
