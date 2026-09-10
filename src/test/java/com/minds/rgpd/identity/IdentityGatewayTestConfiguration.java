package com.minds.rgpd.identity;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class IdentityGatewayTestConfiguration {

    @Bean
    @Primary
    public FakeIdentityGateway fakeIdentityGateway() {
        return new FakeIdentityGateway();
    }
}
