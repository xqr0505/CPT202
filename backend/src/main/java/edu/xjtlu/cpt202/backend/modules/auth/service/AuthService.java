package edu.xjtlu.cpt202.backend.modules.auth.service;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginResponse;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RegisterRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.ResetPasswordRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendResetCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.VerifyResetCodeRequest;

public interface AuthService {

    void sendVerificationCode(SendVerificationCodeRequest request);

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void sendResetPasswordCode(SendResetCodeRequest request);

    void verifyResetCode(VerifyResetCodeRequest request);  
    
    void resetPassword(ResetPasswordRequest request);

}
