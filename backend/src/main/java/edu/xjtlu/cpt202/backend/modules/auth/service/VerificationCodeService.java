package edu.xjtlu.cpt202.backend.modules.auth.service;

import edu.xjtlu.cpt202.backend.modules.auth.model.entity.VerificationCode;

public interface VerificationCodeService {

    void sendCode(String email, String type, String subject, String contentTemplate);

    VerificationCode requireLatestValidCode(String email, String type, String code, String invalidMessage);

    void markCodeUsed(VerificationCode verificationCode);
}
