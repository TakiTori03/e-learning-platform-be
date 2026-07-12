package com.hust.commonlibrary.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import com.hust.commonlibrary.constants.MessageKeys;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 5xx Internal Errors
    UNCATEGORIZED_EXCEPTION(500, MessageKeys.APP_UNCATEGORIZED_500, HttpStatus.INTERNAL_SERVER_ERROR),

    // 4xx Client Errors
    INVALID_KEY(1001, MessageKeys.APP_AUTHORIZATION_403, HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, MessageKeys.USER_EXISTED, HttpStatus.CONFLICT), // Updated status to CONFLICT
    USERNAME_INVALID(1003, MessageKeys.USER_ID_REQUIRED, HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, MessageKeys.PASSWORD_REQUIRED, HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, MessageKeys.USER_NOT_FOUND, HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, MessageKeys.APP_AUTHORIZATION_403, HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, MessageKeys.APP_AUTHORIZATION_403, HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Ngày sinh không hợp lệ. Bạn phải trên {min} tuổi", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_ACTIVED(1009, MessageKeys.ACCOUNT_NOT_ACTIVATED, HttpStatus.FORBIDDEN),
    ACCOUNT_ACTIVATED(1013, MessageKeys.ACCOUNT_ACTIVATED, HttpStatus.BAD_REQUEST),
    EMAIL_PASSWORD_NOT_MATCH(1010, MessageKeys.PASSWORD_NOT_MATCH, HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1011, MessageKeys.TOKEN_INVALID, HttpStatus.BAD_REQUEST),
    SESSION_NOT_EXISTED(1012, MessageKeys.SESSION_NOT_EXISTED, HttpStatus.BAD_REQUEST),
    RESET_TOKEN_ALREADY_SENT(1014, MessageKeys.ACCOUNT_RESETKEY_ALREADY_SENT, HttpStatus.BAD_REQUEST),
    ACCOUNT_IS_DELETED(1015, MessageKeys.ACCOUNT_IS_DELETED, HttpStatus.GONE),
    CONCURRENT_REQUEST(1016, "Hệ thống đang bận xử lý yêu cầu trước đó, vui lòng thử lại sau", HttpStatus.TOO_MANY_REQUESTS),
    TOO_MANY_REQUESTS(1017, "Bạn đã vượt quá giới hạn lượt gọi cho phép, vui lòng chậm lại", HttpStatus.TOO_MANY_REQUESTS),

    // Validation
    VALIDATION_ERROR(400, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),

    // Keycloak Specific (More granular if needed)
    KEYCLOAK_ERROR(1020, "Lỗi kết nối tới xác thực hệ thống", HttpStatus.SERVICE_UNAVAILABLE),
    KEYCLOAK_USER_CONFLICT(1021, "Email này đã được sử dụng", HttpStatus.CONFLICT),
    KEYCLOAK_ROLE_NOT_FOUND(1022, "Role này không tồn tại", HttpStatus.NOT_FOUND);



    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
