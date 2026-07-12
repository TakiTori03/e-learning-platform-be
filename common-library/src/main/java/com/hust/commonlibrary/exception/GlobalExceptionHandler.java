package com.hust.commonlibrary.exception;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hust.commonlibrary.constants.MessageKeys;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.exception.payload.ExpiredTokenException;
import com.hust.commonlibrary.exception.payload.InvalidParamException;
import com.hust.commonlibrary.exception.payload.RefreshTokenException;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.exception.payload.VerificationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    /**
     * Fallback for all unhandled Runtime Exceptions (500)
     */
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Void>> handlingException(Exception exception) {
        log.error("Unhandled Exception: ", exception);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode()))
                .error(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }

    /**
     * Illegal argument exceptions (400)
     */
    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("Illegal Argument Exception: {}", exception.getMessage());
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message("400")
                .error(exception.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }

    /**
     * Catch-all for our custom application exceptions
     */
    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(errorCode.getCode()))
                .error(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    /**
     * Validation errors (@Valid)
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handlingValidation(MethodArgumentNotValidException exception) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;

        // Thu thập danh sách chi tiết các lỗi validation
        java.util.List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(errorCode.getCode()))
                .error(errorCode.getMessage())
                .errors(details) // Gắn danh sách lỗi vào đây
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    /**
     * Auth and Refresh Token Errors
     */
    @ExceptionHandler({ AccessDeniedException.class, RefreshTokenException.class })
    public ResponseEntity<ApiResponse<Void>> handlingAccessDeniedException(Exception exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(errorCode.getCode()))
                .error(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    /**
     * Login / Token Verification Errors
     */
    @ExceptionHandler({ AuthenticationException.class, VerificationException.class, ExpiredTokenException.class })
    public ResponseEntity<ApiResponse<Void>> authenticationException(Exception e) {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(errorCode.getCode()))
                .error(e.getMessage() != null ? e.getMessage() : errorCode.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiResponse);
    }

    /**
     * Entity specific exceptions
     */
    @ExceptionHandler(value = { ResourceNotFoundException.class, InvalidParamException.class })
    public ResponseEntity<ApiResponse<Void>> handleSpecificExceptions(Exception e) {
        HttpStatus status = (e instanceof ResourceNotFoundException) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .message(String.valueOf(status.value()))
                .error(e.getMessage() != null ? e.getMessage() : MessageKeys.ERROR_MESSAGE)
                .build();
        return ResponseEntity.status(status).body(apiResponse);
    }
}
