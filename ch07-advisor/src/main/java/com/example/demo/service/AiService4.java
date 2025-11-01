package com.example.demo.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import com.example.demo.advisor.MaxCharLengthAdvisor;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService4 {
  private ChatClient chatClient;

  public AiService4(ChatClient.Builder chatClientBuilder) {
    SafeGuardAdvisor safeGuardAdvisor = new SafeGuardAdvisor(
      List.of("욕설", "계좌번호", "폭력", "폭탄"),
      "해당 질문은 민감한 콘텐츠 요청이므로 응답할 수 없습니다.",
      Ordered.HIGHEST_PRECEDENCE
    );

    this.chatClient = chatClientBuilder
      .defaultAdvisors(safeGuardAdvisor)
      .build();
  }

  public String advisorSafeGuard(String question) {
    String response = chatClient.prompt()
      .user(question)
      .advisors(advisorSpec -> advisorSpec.param(MaxCharLengthAdvisor.MAX_CHAR_LENGTH, 100))
      .call()
      .content();

    return response;
  }

}
