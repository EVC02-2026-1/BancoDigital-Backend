package com.banco.digital.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = "http://localhost:5173/reset-password/" + resetToken;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@bancodigital.com");
        message.setTo(toEmail);
        message.setSubject("Recuperación de contraseña - Banco Digital");
        message.setText("Hola,\n\nHas solicitado recuperar tu contraseña. "
                + "Por favor, haz clic en el siguiente enlace para restablecerla:\n\n"
                + resetUrl + "\n\n"
                + "Este enlace expirará en 30 minutos.\n"
                + "Si no solicitaste este cambio, por favor ignora este correo.");
        
        mailSender.send(message);
    }
}
