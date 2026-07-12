package com.hust.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * BFF Filter: Tự động bóc tách JWT từ HttpOnly Cookie "access_token" 
 * và gắn vào Header "Authorization: Bearer <token>" trước khi gửi xuống các microservice nghiệp vụ.
 */
@Component
@Slf4j
public class BffCookieFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<HttpCookie> cookies = exchange.getRequest().getCookies().get("access_token");
        if (cookies != null && !cookies.isEmpty()) {
            String token = cookies.get(0).getValue();
            log.debug("BFF: Extracting token from cookie and adding to Authorization header for internal call");
            
            // Gắn vào Header cho các service phía sau
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.header("Authorization", "Bearer " + token))
                    .build();
            
            return chain.filter(mutatedExchange);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1; // Chạy sớm nhất trước các filter khác
    }
}
