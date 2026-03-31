package edu.xjtlu.cpt202.backend.common.constant;

/**
 * Authentication & Authorization Related Constants.
 * @author QiranXiao
 * @date 2026/03/31
 */
public class SecurityConstant {

    /**
     * The HTTP header name where the JWT token is expected.
     */
    public static final String JWT_TOKEN_HEADER = "Authorization";

    /**
     * The prefix prepended to the JWT token string in the Authorization header.
     */
    public static final String JWT_TOKEN_PREFIX = "Bearer ";

    /**
     * The secret key used for signing and verifying JWT tokens.
     * This must be configured via application properties for production.
     */
    public static final String JWT_SECRET_KEY = "[YourStrongSecretKey]";

    /**
     * The default expiration time for JWT Access Tokens, in milliseconds (1 hour).
     */
    public static final Long JWT_EXPIRATION_MILLISECONDS = 3600000L;

    /**
     * The expiration time for Refresh Tokens, in days.
     */
    public static final Integer REFRESH_TOKEN_EXPIRATION_DAYS = 30;

    /**
     * The expiration time for "Remember Me" functionality, in days.
     */
    public static final Integer REMEMBER_ME_EXPIRATION_DAYS = 7;

    /**
     * The maximum number of consecutive failed login attempts before an account is locked.
     */
    public static final Integer MAX_LOGIN_ATTEMPTS = 5;

    /**
     * The duration, in minutes, for which an account remains locked after exceeding MAX_LOGIN_ATTEMPTS.
     */
    public static final Integer ACCOUNT_LOCK_DURATION_MINUTES = 30;

    /**
     * The expiration time for email verification codes, in minutes.
     */
    public static final Integer VERIFICATION_CODE_EXPIRATION_MINUTES = 5;

    /**
     * The length of the generated numeric verification code.
     */
    public static final Integer VERIFICATION_CODE_LENGTH = 6;

    /**
     * The cooldown period, in seconds, before a user can request a new verification code.
     */
    public static final Integer VERIFICATION_CODE_RESEND_COOLDOWN_SECONDS = 60;

    /**
     * The default role assigned to a new user upon registration.
     */
    public static final String DEFAULT_ROLE = "CUSTOMER";
}

