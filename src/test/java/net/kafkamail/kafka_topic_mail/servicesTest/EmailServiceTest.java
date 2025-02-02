package net.kafkamail.kafka_topic_mail.servicesTest;

import net.kafkamail.kafka_topic_mail.service.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class EmailServiceTest {

    @Test
    void testSendEmail() {
        // Mock JavaMailSender
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService emailService = new EmailService(mailSender);

        // Capture the sent email message
        ArgumentCaptor<SimpleMailMessage> emailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Call method
        emailService.sendEmail("Test Kafka Message");

        // Verify email was sent
        verify(mailSender, times(1)).send(emailCaptor.capture());

        // Validate email content
        SimpleMailMessage sentEmail = emailCaptor.getValue();
        assertEquals("Kafka Message Received", sentEmail.getSubject());
        assertEquals("Test Kafka Message", sentEmail.getText());
    }
}
