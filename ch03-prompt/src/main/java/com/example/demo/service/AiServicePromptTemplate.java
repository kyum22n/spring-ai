package com.example.demo.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AiServicePromptTemplate {
  // ChatClient 얻기
  private ChatClient chatClient;
  
  // SystemPromptTemplate 생성
  private PromptTemplate systemTemplate = SystemPromptTemplate.builder()
  .template("""
    답변을 생성할 때 HTML과 CSS를 사용해서 파란 글자로 출력하세요.
    <span> 태그 안에 들어갈 내용만 출력하세요.
    """)
    .build();
    
  // PromptTemplate생성
  private PromptTemplate userTemplate = PromptTemplate.builder()
    .template("""
      다음 한국어 문장을 {language}로 반드시 번역한 후 답변을 해주세요.
      {statement}
    """)
    .build();

  // ChatClient 얻기(생성자)
  public AiServicePromptTemplate(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // UserMessage만 포함된 프롬프트를 LLM에 전달
  public Flux<String> promptTemplate1(String language, String statement) {
    Prompt prompt = userTemplate.create(
      Map.of("language", language, "statement", statement)
    );

    Flux<String> fluxString = chatClient.prompt(prompt)
      .stream()
      .content();

    return fluxString;
  }

  // 메시지 객체를 만들어서
  // UserMessage와 SystemMessage가 함께 포함된 프롬프트를 LLM에 전달
  public Flux<String> promptTemplate2(String language, String statement) {
    Flux<String> response = chatClient.prompt()
    .messages(
      // Message 객체를 얻고 messages에 제공
      systemTemplate.createMessage(),
      userTemplate.createMessage(
          Map.of("language", language, "statement", statement)
        )
        )
        .stream()
        .content();
        
        return response;
      }
      
  // UserMessage와 SystemMessage를 각각의 템플릿에서 얻어
  // system()과 user()에 제공
  public Flux<String> promptTemplate3(String language, String statement) {
    Flux<String> resposne = chatClient.prompt()
      .system(systemTemplate.render())
      .user(userTemplate.render(Map.of(
        "language", language, "statement", statement
      )))
      .stream()
      .content();

    return resposne;
  }

  // 매개변수화된 문자열을 만들어 데이터를 빠르게 바인딩
  public Flux<String> promptTemplate4(String language, String statement) {
    // 시스템 메시지
    String systemText = """
      답변을 생성할 때 HTML과 CSS를 사용해서 파란 글자로 출력하세요
      <span> 태그 안에 들어갈 내용만 출력하세요.
    """;

    // 사용자 메시지
    String userText = """
      다음 한국어 문장을 %s로 번역해 주세요.\n 문장: %s
      """.formatted(language, statement);

    Flux<String> response = chatClient.prompt()
      .system(systemText)
      .user(userText)
      .stream()
      .content();

    return response;
  }
  

}
