package edu.xjtlu.cpt202.backend.modules.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.xjtlu.cpt202.backend.common.constant.SecurityConstant;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import edu.xjtlu.cpt202.backend.common.exception.BusinessException;
import edu.xjtlu.cpt202.backend.modules.auth.mapper.VerificationCodeMapper;
import edu.xjtlu.cpt202.backend.modules.auth.model.entity.VerificationCode;
import edu.xjtlu.cpt202.backend.modules.auth.service.VerificationCodeService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeServiceImpl.class);

    private final VerificationCodeMapper verificationCodeMapper;
    private final JavaMailSender mailSender;
    private final Environment env;

    @Override
    public void sendCode(String email, String type, String subject, String contentTemplate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownStart =
                now.minusSeconds(SecurityConstant.VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS);

        Long recentCount = verificationCodeMapper.selectCount(
                new QueryWrapper<VerificationCode>()
                        .eq("email", email)
                        .eq("type", type)
                        .gt("created_at", cooldownStart)
        );
        if (recentCount != null && recentCount > 0) {
            throw new BusinessException(
                    ResultCodeEnum.DUPLICATE_REQUEST.getCode(),
                    "Please wait " + SecurityConstant.VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS + " seconds"
            );
        }

        String code = String.format(
                "%0" + SecurityConstant.VERIFICATION_CODE_LENGTH + "d",
                ThreadLocalRandom.current().nextInt(0, (int) Math.pow(10, SecurityConstant.VERIFICATION_CODE_LENGTH))
        );

        VerificationCode verificationCode = VerificationCode.builder()
                .email(email)
                .code(code)
                .type(type)
                .isUsed(false)
                .expiresAt(now.plusMinutes(SecurityConstant.VERIFICATION_CODE_EXPIRATION_MINUTES))
                .build();
        verificationCodeMapper.insert(verificationCode);

        try {
            sendEmail(
                    email,
                    subject,
                    String.format(contentTemplate, code, SecurityConstant.VERIFICATION_CODE_EXPIRATION_MINUTES)
            );
            logger.info("Verification code sent successfully: email={}, type={}", email, type);
        } catch (Exception exception) {
            logger.error(
                    "Failed to send verification code email: email={}, type={}, reason={}",
                    email,
                    type,
                    exception.getMessage(),
                    exception
            );

            if (verificationCode.getId() != null) {
                verificationCodeMapper.deleteById(verificationCode.getId());
            }

            throw new BusinessException(
                    ResultCodeEnum.SYSTEM_ERROR.getCode(),
                    "Failed to send verification email"
            );
        }
    }

    @Override
    public VerificationCode requireLatestValidCode(String email, String type, String code, String invalidMessage) {
        LocalDateTime now = LocalDateTime.now();
        String normalizedCode = code == null ? "" : code.trim();

        VerificationCode codeRecord = verificationCodeMapper.selectOne(
                new QueryWrapper<VerificationCode>()
                        .eq("email", email)
                        .eq("type", type)
                        .eq("is_used", false)
                        .orderByDesc("created_at")
                        .last("LIMIT 1")
        );

        if (codeRecord == null) {
            throw new BusinessException(ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(), invalidMessage);
        }

        if (codeRecord.getExpiresAt() == null || codeRecord.getExpiresAt().isBefore(now)) {
            throw new BusinessException(
                    ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(),
                    "Verification code has expired. Please request a new one."
            );
        }

        if (!normalizedCode.equals(codeRecord.getCode())) {
            throw new BusinessException(
                    ResultCodeEnum.AUTH_ERROR_BLOCK.getCode(),
                    "Verification code is incorrect"
            );
        }

        return codeRecord;
    }

    @Override
    public void markCodeUsed(VerificationCode verificationCode) {
        if (verificationCode == null || verificationCode.getId() == null) {
            return;
        }
        verificationCode.setIsUsed(true);
        verificationCodeMapper.updateById(verificationCode);
    }

    private void sendEmail(String to, String subject, String content) throws Exception {
        if (mailSender == null) {
            logger.error("Mail sender not configured");
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Mail service is unavailable");
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        if (mimeMessage == null) {
            logger.error("Mail sender returned null MimeMessage");
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR.getCode(), "Mail service is unavailable");
        }

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
        String fromAddress = env == null ? null : env.getProperty("spring.mail.username");
        if (StrUtil.isBlank(fromAddress)) {
            fromAddress = "noreply@example.com";
        }

        helper.setFrom("ExpertLink <" + fromAddress + ">");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content);

        mailSender.send(mimeMessage);
    }
}
