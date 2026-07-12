package com.hust.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter to block any external requests containing '/internal/' in their path.
 * Internal APIs are only intended for service-to-service communication within the trusted network.
 */
@Component
@Slf4j
public class InternalApiFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        if (path.contains("/internal/")) {
            log.warn("Security Alert: External request attempted to access internal API: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // High priority - run this filter before authentication or routing
        return -100;
    }
}
