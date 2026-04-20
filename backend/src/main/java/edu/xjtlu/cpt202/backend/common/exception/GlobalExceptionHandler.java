package edu.xjtlu.cpt202.backend.common.exception;

import edu.xjtlu.cpt202.backend.common.result.Result;
import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.stream.Collectors;
/**
 * global exception handler
 * @author QiranXiao
 * @date 2026/3/26
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String FILE_PART_NAME = "file";

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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), e.getParameterName() + " is required");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        logger.warn("Multipart request is missing required part '{}': {}", e.getRequestPartName(), e.getMessage());
        if (FILE_PART_NAME.equals(e.getRequestPartName())) {
            return Result.fail(
                    ResultCodeEnum.BAD_REQUEST.getCode(),
                    "Avatar upload is missing the file field. Send the image as FormData with the field name 'file'."
            );
        }
        return Result.fail(ResultCodeEnum.BAD_REQUEST.getCode(), e.getRequestPartName() + " is required");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), ResultCodeEnum.PARAM_ERROR.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return Result.fail(ResultCodeEnum.BAD_REQUEST.getCode(), "Uploaded image is too large. Please choose a smaller file.");
    }

    @ExceptionHandler(MultipartException.class)
    public Result<Void> handleMultipartException(MultipartException e) {
        logger.warn("Invalid multipart upload request: {}", e.getMessage(), e);

        String normalizedMessage = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (normalizedMessage.contains("not a multipart request")
                || normalizedMessage.contains("content type")
                || normalizedMessage.contains("boundary")) {
            return Result.fail(
                    ResultCodeEnum.BAD_REQUEST.getCode(),
                    "Avatar upload request must use multipart/form-data. Send the image as FormData with field name 'file' and do not set the Content-Type header manually."
            );
        }

        return Result.fail(
                ResultCodeEnum.BAD_REQUEST.getCode(),
                "Invalid multipart upload request. Send the image as FormData with field name 'file'."
        );
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR.getCode(), ResultCodeEnum.SYSTEM_ERROR.getMessage());
    }

}
