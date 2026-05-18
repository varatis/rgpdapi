package com.minds.rgpd.business.services.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class HelloWorldServiceImplTest {
    @InjectMocks
    private HelloWorldServiceImpl helloWorldService;
    
    @Test
    void getHelloWorld() {

        // WHEN
        String resultat = helloWorldService.getHelloWorld();

        // THEN
        assertEquals("Hello World", resultat);
    }
}
