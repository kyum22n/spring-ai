package com.example.demo.internetsearch;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InternetSearchService {
  // ##### 필드 #####
  private ChatClient chatClient;

  @Autowired
  private InternetSearchTools internetSearchTools;

  // ##### 생성자 #####
  public InternetSearchService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // ##### LLM과 대화하는 메소드 #####
  public String chat(String question) {
    String answer = this.chatClient.prompt()
        .system("""
            HTML과 CSS를 사용해서 들여쓰기가 된 답변을 출력하세요.
            <div>에 들어가는 내용으로만 답변을 주세요. <h1>, <h2>, <h3>태그는 사용하지 마세요.
            """)    
        .user(question)
        .tools(internetSearchTools)
        .call()
        .content();
    return answer;
  }
}
