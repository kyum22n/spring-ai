package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AiServiceByChatClient {
  private ChatClient chatClient;

  public AiServiceByChatClient(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String generateText(String question) {
    String answer = chatClient.prompt()
      // prompt 생성
      .system("사용자 질문에 대해 한국어로 답변을 해야합니다.")
      .user(question)
      .options(ChatOptions.builder()
        .model("gpt-4o-mini")
        .maxTokens(300)
        .temperature(0.5)
        .build()
      )
      // LLM 응답
      .call()
      // answer를 리턴
      .content();

    return answer;
  }
  
  public Flux<String> generateStreamText(String question) {
    Flux<String> fluxString = chatClient.prompt()
      // prompt 생성
      .system("사용자 질문에 대해 한국어로 답변을 해야합니다.")
      .user(question)
      .options(ChatOptions.builder()
        .model("gpt-4o-mini")
        // .maxTokens(300)
        .temperature(0.5)
        .build()
      )
      // LLM 응답
      .stream()
      // answer를 리턴
      .content();
  
    return fluxString;
  }

}
