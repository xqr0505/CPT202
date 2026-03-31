package edu.xjtlu.cpt202.backend.common.exception;

import edu.xjtlu.cpt202.backend.common.enums.ResultCodeEnum;

/**
 * @author QiranXiao
 * @date 2026/3/26
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ResultCodeEnum resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
