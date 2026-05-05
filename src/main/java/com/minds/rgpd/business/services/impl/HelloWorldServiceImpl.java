package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.services.HelloWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelloWorldServiceImpl implements HelloWorldService {
  @Override
  public String getHelloWorld() {
    return "Hello World";
  }
}
