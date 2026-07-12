package com.hust.identityservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = IdentityServiceApplication.class, properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class IdentityServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
