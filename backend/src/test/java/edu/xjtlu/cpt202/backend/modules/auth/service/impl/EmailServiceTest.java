package edu.xjtlu.cpt202.backend.modules.auth.service.impl;

import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.service.AuthService;
import edu.xjtlu.cpt202.backend.modules.auth.mapper.VerificationCodeMapper;
import edu.xjtlu.cpt202.backend.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;



@SpringBootTest
@Transactional  // 测试结束后自动回滚数据库操作，避免产生垃圾数据
public class EmailServiceTest {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AuthService authService;

    @Autowired
    private VerificationCodeMapper verificationCodeMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 测试1：直接使用 JavaMailSender 发送简单邮件
     * 目的：验证 SMTP 配置、授权码、网络连接是否正确
     */
    @Test
    @DisplayName("Test JavaMailSender basic email sending")
    void testSendSimpleEmail() {
        String toEmail = "Danyi.Huang23@student.xjtlu.edu.cn";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("2906326615@qq.com");
        message.setTo(toEmail);
        message.setSubject("Mail Service Test");
        message.setText("If you receive this email, SMTP configuration is working correctly.");

        assertDoesNotThrow(() -> mailSender.send(message),
                "Failed to send email. Check SMTP configuration.");
    }

    /**
     * 测试2：完整调用 AuthService.sendVerificationCode
     * 目的：验证验证码生成、数据库存储、邮件发送整个链路
     * 注意：因为添加了 @Transactional，测试结束后插入的验证码记录会被回滚，不会污染数据库
     */
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