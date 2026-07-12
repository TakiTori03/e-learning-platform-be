package com.hust.commonlibrary.config;

import com.hust.commonlibrary.security.RealtimeUserStatusValidator;
import com.hust.commonlibrary.security.TokenBlacklistValidator;
import com.hust.commonlibrary.service.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

@AutoConfiguration(after = BaseRedisConfig.class)
@ConditionalOnClass(Jwt.class)
@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
@ConditionalOnBean(RedisService.class)
public class CommonJwtDecoderAutoConfiguration {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(RedisService redisService) {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> userStatusValidator = new RealtimeUserStatusValidator(redisService);
        OAuth2TokenValidator<Jwt> tokenBlacklistValidator = new TokenBlacklistValidator(redisService);

        OAuth2TokenValidator<Jwt> delegatingValidator = new DelegatingOAuth2TokenValidator<>(
                defaultValidator, 
                userStatusValidator,
                tokenBlacklistValidator
        );

        jwtDecoder.setJwtValidator(delegatingValidator);
        return jwtDecoder;
    }
}
