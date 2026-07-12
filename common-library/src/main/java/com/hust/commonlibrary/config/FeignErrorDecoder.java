package com.hust.commonlibrary.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.commonlibrary.dto.ApiResponse;
import com.hust.commonlibrary.exception.AppException;
import com.hust.commonlibrary.exception.ErrorCode;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
@ConditionalOnClass(name = "feign.codec.ErrorDecoder")
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        try (InputStream inputStream = response.body().asInputStream()) {
            ApiResponse<?> apiResponse = objectMapper.readValue(inputStream, ApiResponse.class);
            
            if (apiResponse != null && apiResponse.getMessage() != null) {
                log.error("Feign Error Decoder [{}]: Status {}, Message/Code {}", 
                        methodKey, response.status(), apiResponse.getMessage());
                
                try {
                    int code = Integer.parseInt(apiResponse.getMessage());
                    for (ErrorCode errorCode : ErrorCode.values()) {
                        if (errorCode.getCode() == code) {
                            return new AppException(errorCode);
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("Error message format is not numeric: {}", apiResponse.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to decode Feign error response body", e);
        }

        return defaultErrorDecoder.decode(methodKey, response);
    }
}
