package edu.xjtlu.cpt202.backend.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.auth.mapper.VerificationCodeMapper;
import edu.xjtlu.cpt202.backend.modules.auth.model.entity.VerificationCode;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceImplTest {

    @Mock
    private VerificationCodeMapper verificationCodeMapper;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Environment env;

    @InjectMocks
    private VerificationCodeServiceImpl verificationCodeService;

    @Test
    void requireLatestValidCode_returnsRecordWhenCodeMatchesAndNotExpired() {
        VerificationCode codeRecord = VerificationCode.builder()
                .id(10L)
                .email("alice@example.com")
                .type("REGISTER")
                .code("123456")
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(verificationCodeMapper.selectOne(any(QueryWrapper.class))).thenReturn(codeRecord);

        VerificationCode result = verificationCodeService.requireLatestValidCode(
                "alice@example.com",
                "REGISTER",
                " 123456 ",
                "Invalid verification code"
        );

        assertSame(codeRecord, result);
        verify(verificationCodeMapper).selectOne(any(QueryWrapper.class));
        verifyNoMoreInteractions(verificationCodeMapper);
        verifyNoInteractions(mailSender, env);
    }

    @Test
    void requireLatestValidCode_whenNoRecord_throwsConfiguredInvalidMessage() {
        when(verificationCodeMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationCodeService.requireLatestValidCode(
                        "alice@example.com",
                        "REGISTER",
                        "123456",
                        "Invalid verification code"
                )
        );

        assertEquals(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), exception.getCode());
        assertEquals("Invalid verification code", exception.getMessage());
        verify(verificationCodeMapper).selectOne(any(QueryWrapper.class));
        verifyNoMoreInteractions(verificationCodeMapper);
    }

    @Test
    void requireLatestValidCode_whenExpired_throwsExpiredMessage() {
        VerificationCode codeRecord = VerificationCode.builder()
                .id(10L)
                .code("123456")
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();
        when(verificationCodeMapper.selectOne(any(QueryWrapper.class))).thenReturn(codeRecord);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationCodeService.requireLatestValidCode(
                        "alice@example.com",
                        "REGISTER",
                        "123456",
                        "Invalid verification code"
                )
        );

        assertEquals(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), exception.getCode());
        assertEquals("Verification code has expired. Please request a new one.", exception.getMessage());
    }

    @Test
    void requireLatestValidCode_whenCodeDoesNotMatch_throwsIncorrectMessage() {
        VerificationCode codeRecord = VerificationCode.builder()
                .id(10L)
                .code("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(verificationCodeMapper.selectOne(any(QueryWrapper.class))).thenReturn(codeRecord);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationCodeService.requireLatestValidCode(
                        "alice@example.com",
                        "REGISTER",
                        "000000",
                        "Invalid verification code"
                )
        );

        assertEquals(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), exception.getCode());
        assertEquals("Verification code is incorrect", exception.getMessage());
    }

    @Test
    void sendCode_whenRecentCodeExists_throwsDuplicateRequestAndDoesNotSendEmail() {
        when(verificationCodeMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> verificationCodeService.sendCode(
                        "alice@example.com",
                        "REGISTER",
                        "Subject",
                        "Code %s expires in %d minutes"
                )
        );

        assertEquals(ResultCodeEnum.DUPLICATE_REQUEST.getCode(), exception.getCode());
        assertTrue(exception.getMessage().startsWith("Please wait "));
        verify(verificationCodeMapper).selectCount(any(QueryWrapper.class));
        verify(verificationCodeMapper, never()).insert(any(VerificationCode.class));
        verifyNoInteractions(mailSender, env);
    }

    @Test
    void sendCode_insertsCodeAndSendsEmailWhenNotInCooldown() {
        when(verificationCodeMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        when(env.getProperty("spring.mail.username")).thenReturn("noreply@example.com");

        assertDoesNotThrow(() -> verificationCodeService.sendCode(
                "alice@example.com",
                "REGISTER",
                "Subject",
                "Code %s expires in %d minutes"
        ));

        ArgumentCaptor<VerificationCode> codeCaptor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeMapper).selectCount(any(QueryWrapper.class));
        verify(verificationCodeMapper).insert(codeCaptor.capture());
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));

        VerificationCode savedCode = codeCaptor.getValue();
        assertEquals("alice@example.com", savedCode.getEmail());
        assertEquals("REGISTER", savedCode.getType());
        assertEquals(false, savedCode.getIsUsed());
        assertEquals(6, savedCode.getCode().length());
    }

    @Test
    void markCodeUsed_updatesExistingCodeRecord() {
        VerificationCode codeRecord = VerificationCode.builder()
                .id(10L)
                .isUsed(false)
                .build();

        verificationCodeService.markCodeUsed(codeRecord);

        assertEquals(true, codeRecord.getIsUsed());
        verify(verificationCodeMapper).updateById(codeRecord);
    }

    @Test
    void markCodeUsed_ignoresNullOrUnsavedRecord() {
        verificationCodeService.markCodeUsed(null);
        verificationCodeService.markCodeUsed(VerificationCode.builder().isUsed(false).build());

        verifyNoInteractions(verificationCodeMapper);
    }
}
