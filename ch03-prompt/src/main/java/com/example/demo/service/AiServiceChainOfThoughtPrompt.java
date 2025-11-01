package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AiServiceChainOfThoughtPrompt {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiServiceChainOfThoughtPrompt(ChatClient.Builder chatClientBuilder) {
    chatClient = chatClientBuilder.build();
  }

  // ##### 메소드 #####
  public Flux<String> chainOfThought(String question) {
    Flux<String> answer = chatClient.prompt()
        .user("""
            %s
            
            """.formatted(question))
        .stream()
        .content();
    return answer;
  }
}
