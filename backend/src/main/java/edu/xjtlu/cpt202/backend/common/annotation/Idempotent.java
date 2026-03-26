package edu.xjtlu.cpt202.backend.common.annotation;

import java.lang.annotation.*;

/**
 * Annotation for idempotent requests.
 * Used to prevent duplicate submissions from the same user within a short time.
 * @author QiranXiao
 * @date 2026/3/26
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * Timeout for idempotency check (in seconds).
     * Requests within this duration will be rejected.
     */
    int timeout() default 3;

    /**
     * Error message when a duplicate request is detected.
     */
    String message() default "Duplicate request detected, please try again later.";
}
