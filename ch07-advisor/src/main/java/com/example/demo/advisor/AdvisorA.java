package com.example.demo.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AdvisorA implements CallAdvisor {
  @Override
  public String getName() {
    return this.getClass().getSimpleName(); // "AdvisorA"
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 1;  // 우선순위
  }

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {

    // 전처리
    // ...
    log.info("[전처리]");
    
    ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
    
    // 후처리
    // ...
    log.info("[후처리]");

    return chatClientResponse;
  }
}
