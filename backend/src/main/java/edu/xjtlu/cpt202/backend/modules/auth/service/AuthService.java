package edu.xjtlu.cpt202.backend.modules.auth.service;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.LoginResponse;
import edu.xjtlu.cpt202.backend.modules.auth.dto.RegisterRequest;
import edu.xjtlu.cpt202.backend.modules.auth.dto.SendVerificationCodeRequest;

public interface AuthService {

    void sendVerificationCode(SendVerificationCodeRequest request);

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

}
