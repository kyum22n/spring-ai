package com.example.demo.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RagService1 {
  // ChatClient
  private ChatClient chatClient;
  // VectorStore
  @Autowired
  private VectorStore vectorStore;
  // JdbcTemplate
  @Autowired
  JdbcTemplate jdbcTemplate;

  // 생성자 주입
  public RagService1(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
        // .defaultAdvisors(null)
        .build();
  }

  // VectorStore 비우기
  public void clearVectorStore() {
    jdbcTemplate.update("TRUNCATE TABLE vector_store"); // 테이블 비우는 sql문을 직접 작성
  }

  // PDF 파일을 ETL
  public void ragEtl(MultipartFile attach, String source, int chunkSize, int minChunkSizeChars) throws Exception {

    // 추출
    // Resource 생성
    Resource resource = new ByteArrayResource(attach.getBytes());
    // DocumentReader 생성
    DocumentReader reader = new PagePdfDocumentReader(resource);
    // DocumentReader reader = new TextReader(resource);
    //
    List<Document> documents = reader.read();

    // 메타데이터 추가
    for (Document doc : documents) {
      doc.getMetadata().put("source", source);
    }

    // 쪼개기
    TokenTextSplitter splitter = new TokenTextSplitter(
        chunkSize, minChunkSizeChars, 0, 10000, true);
    documents = splitter.apply(documents);

    // 적재
    vectorStore.add(documents);
  }

  // LLM과 대화
  public String ragChat(String question, double score, String source) {

    // SearchRequest 생성 - 검색 조건
    Builder builder = SearchRequest.builder()
        .similarityThreshold(score) // 유사 점수
        .topK(3); // 3개 검색
    if (source != null && !source.equals("")) {
      // source값이 있을 경우 추가해서 검색 조건 만듦
      builder.filterExpression("source == '%s'".formatted(source));
    }
    SearchRequest searchRequest = builder.build();

    // Advisor 얻기
    QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
        .searchRequest(searchRequest)
        .build();

    // 모델 호출 및 응답 - RAG 수행
    String answer = chatClient.prompt()
        .user(question)
        .advisors(advisor, new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
        .call()
        .content();

    return answer;
  }

}
