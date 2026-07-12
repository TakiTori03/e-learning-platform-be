package com.hust.commonlibrary.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class AppException extends RuntimeException {
    private ErrorCode errorCode;
}
