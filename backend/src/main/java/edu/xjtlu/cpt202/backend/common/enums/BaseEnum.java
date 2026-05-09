package edu.xjtlu.cpt202.backend.common.enums;

/**
 * Base interface for enums with a code and description.
 * @author QiranXiao
 * @date 2026/3/26
 */
public interface BaseEnum<T> {

    T getCode();

    String getDesc();
}

