package com.banco.digital.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = frontendUrl + "/reset-password/" + resetToken;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Recuperación de contraseña - Banco Digital");
        message.setText("Hola,\n\nHas solicitado restablecer tu contraseña. Haz clic en el siguiente enlace para continuar:\n\n" 
                        + resetUrl + "\n\nEste enlace expirará en 30 minutos.\n\nSi no solicitaste esto, ignora este correo.");
        
        try {
            mailSender.send(message);
            System.out.println(">>> Correo de recuperación enviado a: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error enviando correo de recuperación: " + e.getMessage());
            // Fallback para desarrollo: imprimir en consola si falla el envío real
            System.out.println("FALLBACK - Enlace de recuperación: " + resetUrl);
        }
    }

    public void sendMfaEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Tu código de seguridad - Banco Digital");
        message.setText("Hola,\n\nTu código de verificación (OTP) es: " + code + "\n\nEste código es válido por 5 minutos.\n\nNo compartas este código con nadie.");
        
        try {
            mailSender.send(message);
            System.out.println(">>> Código OTP enviado a: " + toEmail);
        } catch (Exception e) {
            System.err.println("Error enviando código MFA: " + e.getMessage());
            // Fallback para desarrollo: imprimir en consola si falla el envío real
            System.out.println("FALLBACK - Código OTP: " + code);
        }
    }
}
