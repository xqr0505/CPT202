package edu.xjtlu.cpt202.backend.modules.auth.service.impl;

import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.service.AuthService;
import edu.xjtlu.cpt202.backend.modules.auth.mapper.VerificationCodeMapper;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@Transactional  
public class EmailServiceTest {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AuthService authService;

    @Autowired
    private VerificationCodeMapper verificationCodeMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    @DisplayName("Test JavaMailSender with display name")
    void testSendSimpleEmail() {
        String toEmail = "Danyi.Huang23@student.xjtlu.edu.cn";

        assertDoesNotThrow(() -> {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setFrom("ExpertLink <2906326615@qq.com>");
            helper.setTo(toEmail);
            helper.setSubject("Mail Service Test");
            helper.setText("If you receive this email, SMTP configuration is working correctly.");

            mailSender.send(mimeMessage);
        }, "Failed to send email. Check SMTP configuration.");
    }

    @Test
    @DisplayName("Test full verification code sending flow")
    void testSendVerificationCode() {
        SendVerificationCodeRequest request = new SendVerificationCodeRequest();
        request.setEmail("testuser_" + System.currentTimeMillis() + "@example.com");
        request.setRole("CUSTOMER");
        request.setType("REGISTER");

        assertDoesNotThrow(() -> authService.sendVerificationCode(request),
                "Failed to send verification code, check AuthServiceImpl mail logic");
    }
}