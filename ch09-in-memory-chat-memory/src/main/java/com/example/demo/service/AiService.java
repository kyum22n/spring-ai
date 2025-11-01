package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService {
  // ChatClient 
  private ChatClient chatClient;

  // 생성자 주입
  public AiService(
    ChatClient.Builder chatClientBuilder //,
    // 방법 1
    // ChatMemory chatMemory
    ) {
      
    // 방법 2
    // 메시지 윈도우 변경
    ChatMemory chatMemory = MessageWindowChatMemory.builder()
      // 메모리에 100개까지 저장
      .maxMessages(100)
      .build();

    this.chatClient = chatClientBuilder
      .defaultAdvisors(
        // 대화 기억을 프롬프트에 추가 - 기본 Advisor
        MessageChatMemoryAdvisor
          .builder(chatMemory)
          .build(),
        // LLM 전송 직전에 프롬프트 내용을 출력
        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
      )
      .build();
  }

  // 사용자 질문을 LLM으로 전송하고, 그에 대한 응답을 받음
  public String chat(String question, String conversationId) {
    
    // 모델 호출 및 응답
    String answer = chatClient.prompt()
      .user(question)
      // 공유 데이터 저장
      .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
      .call()
      .content();

    return answer;
  }

}
