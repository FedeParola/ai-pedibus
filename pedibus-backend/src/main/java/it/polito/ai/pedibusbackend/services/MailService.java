package it.polito.ai.pedibusbackend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendConfirmationMail(String to, String confirmUrl) {
        sendMessage(to, "Pedibus account confirmation", "Click the link below to confirm your Pedibus account:\n" + confirmUrl);
    }

    @Async
    public void sendRecoverMail(String to, String recoverUrl) {
        sendMessage(to, "Pedibus account password recovery", "Click the link below to recover your password:\n" + recoverUrl);
    }

    private void sendMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
