package com.hust.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ApiGatewayApplication.class, properties = {
    "eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
