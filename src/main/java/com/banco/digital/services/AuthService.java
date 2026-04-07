package com.banco.digital.services;

import com.banco.digital.dto.AuthRequest;
import com.banco.digital.dto.AuthResponse;
import com.banco.digital.dto.RegisterRequest;
import com.banco.digital.models.Account;
import com.banco.digital.models.User;
import com.banco.digital.repositories.AccountRepository;
import com.banco.digital.repositories.UserRepository;
import com.banco.digital.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String dniFront, String dniBack) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .dniFront(dniFront)
                .dniBack(dniBack)
                .isVerified(false) // Mandatory verification
                .build();

        user = userRepository.save(user);

        // Generar números de cuenta aleatorios de 16 dígitos
        String checkingNumber = java.util.stream.IntStream.range(0, 16)
                .mapToObj(i -> String.valueOf((int) (Math.random() * 10)))
                .reduce("", String::concat);
        
        String savingsNumber = java.util.stream.IntStream.range(0, 16)
                .mapToObj(i -> String.valueOf((int) (Math.random() * 10)))
                .reduce("", String::concat);

        // Crear cuentas por defecto
        Account checking = Account.builder()
                .type("CHECKING")
                .accountNumber(checkingNumber)
                .balance(java.math.BigDecimal.ZERO)
                .user(user)
                .build();

        Account savings = Account.builder()
                .type("SAVINGS")
                .accountNumber(savingsNumber)
                .balance(java.math.BigDecimal.ZERO)
                .user(user)
                .build();

        accountRepository.save(checking);
        accountRepository.save(savings);

        String token = jwtUtils.generateToken(user.getEmail());
        
        user.setAccounts(new ArrayList<>()); // Evitar ciclo infinito JSON si se lazy load
        return new AuthResponse(token, user);
    }

    @Transactional
    public User verifyUser(String email, String selfiePath) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setSelfie(selfiePath);
        user.setVerified(true);
        return userRepository.save(user);
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtils.generateToken(user.getEmail());
        return new AuthResponse(token, user);
    }
    
    @Transactional
    public void generatePasswordResetToken(String email, EmailService emailService) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        String token = java.util.UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(30));
        
        userRepository.save(user);
        
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o no encontrado"));
                
        if (user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("El token ha expirado");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        
        userRepository.save(user);
    }
}
