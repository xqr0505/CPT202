package edu.xjtlu.cpt202.backend.common.exception;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
<<<<<<< Updated upstream
import org.springframework.security.access.AccessDeniedException;
=======
import org.springframework.dao.DataIntegrityViolationException;
>>>>>>> Stashed changes
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;
/**
 * @author QiranXiao
 * @date 2026/3/26
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDeniedException(AccessDeniedException e) {
        throw e;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (message == null || message.isBlank()) {
            message = ResultCodeEnum.PARAM_ERROR.getMessage();
        }
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), message);
    }

<<<<<<< Updated upstream
=======
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        return Result.fail(ResultCodeEnum.BAD_REQUEST.getCode(), "The requested operation violates database constraints");
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public Result<Void> handleNotFoundException(Exception e) {
        return Result.fail(ResultCodeEnum.NOT_FOUND.getCode(), ResultCodeEnum.NOT_FOUND.getMessage());
    }

>>>>>>> Stashed changes
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        // Log the actual exception here in a real application
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR.getCode(), ResultCodeEnum.SYSTEM_ERROR.getMessage());
    }
}
