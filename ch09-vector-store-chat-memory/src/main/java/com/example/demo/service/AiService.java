package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService {
  // ChatClient 
  private ChatClient chatClient;

  // 생성자 주입
  public AiService(
    ChatClient.Builder chatClientBuilder,
    JdbcTemplate jdbcTemplate,
    EmbeddingModel embeddingModel
  ) {
    VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
      // 테이블 자동 생성 false
      .initializeSchema(false)
      // 스키마 이름
      .schemaName("public")
      // 벡터 스토어 테이블 이름
      .vectorTableName("chat_memory_vector_store")
      .build();

    this.chatClient = chatClientBuilder
      .defaultAdvisors(
        VectorStoreChatMemoryAdvisor.builder(vectorStore).build(),
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
