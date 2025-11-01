package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RagService2 {
  
  // 최종 응답을 위한 ChatClient 얻기
  private ChatClient chatClient;
  
  // ChatModel (AI 모델)
  @Autowired
  private ChatModel chatModel;  //gpt-4o-mini

  // ChatMemory (이전 대화 기억)
  @Autowired
  private ChatMemory chatMemory;

  // VectorStore (벡터 저장소)
  @Autowired
  private VectorStore vectorStore;

  // 생성자
  // 최종 응답을 위한 ChatClient 얻기
  public RagService2(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder
      // 로그 출력 내장 Advisor
      .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
      .build();
  }

  //-------------------------------------------------------------------------------
  // ##### CompressionQueryTransformer를 생성하고 반환하는 메소드 #####
  // 검색 전 모듈 생성 메소드 (Advisor에 추가)
  // 이미 MessageChatMemoryAdvisor로부터 이전 대화 내용이 Prompt의 context에 담겨왔기 때문에
  // CompressionQueryTransformer에서는 이전 대화 내용을 별도로 불러올 필요가 없음
  private CompressionQueryTransformer createCompressionQueryTransformer() {
    
    // 사용자 질문을 완전한 질문으로 만들기 위한 새로운 ChatClientBuilder가 필요
    ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
      .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1));

    // 압축 쿼리 변환기 생성
    CompressionQueryTransformer cqt = CompressionQueryTransformer.builder()
      .chatClientBuilder(chatClientBuilder)
      .build();

    return cqt;
  }

  // VectorStoreDocumentRetriever 생성 
  // 검색 모듈 생성 메소드 (Advisor에 추가)
  // 유사도 검색을 수행
  private VectorStoreDocumentRetriever createVectorStoreDocumentRetriever(double score, String source) {
    VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
      // 벡터 저장소 지정
      .vectorStore(vectorStore)
      // 유사도 임계점수를 score로 지정
      .similarityThreshold(score)
      // 상위 3개만 검색
      .topK(3)
      // 
      .filterExpression(() -> {
        // FilterExpressionBuilder 생성
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        if (source != null && !source.equals("")) {
          // 출처가 같으면 검색 조건에 맞는 Expression 타입의 객체를 빌드
          return builder.eq("source", source).build();
        } else {
          return null;
        }
      })
      .build();

    return retriever;
  }

  // LLM과 대화
  public String chatWithCompression(String question, double score, String source, String conversationId) {
    
    // RetrievalAugmentationAdvisor 생성
    // 모호한 사용자 질문을 완전한 질문으로 바꾸기 (LLM)
    // 원래 깡통인데 모듈 추가해서 채워넣음
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
      // 검색 전 모듈 추가 (프롬프트 강화 -> 검색 모듈에 전달)
      .queryTransformers(createCompressionQueryTransformer())
      // 검색 모듈 추가 (검색기이므로 꼭 있어야 함)
      .documentRetriever(createVectorStoreDocumentRetriever(score, source))
      .build(); 

    // LLM에 프롬프트 전송하고 응답 받기
    String answer = chatClient.prompt()
      .user(question)
      .advisors(
        MessageChatMemoryAdvisor.builder(chatMemory).build(),
        retrievalAugmentationAdvisor
      )
      .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
      .call()
      .content();

    return answer;
  }


  //-------------------------------------------------------------------------------
  // ##### RewriteQueryTransformer 생성하고 반환하는 메소드 #####
  private RewriteQueryTransformer createRewriteQueryTransformer() {
    // 새로운 ChatClient 생성하는 빌더 생성
    ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
        .defaultAdvisors(
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)    
        );

    // 질문 재작성기 생성
    RewriteQueryTransformer rewriteQueryTransformer = 
        RewriteQueryTransformer.builder()
            .chatClientBuilder(chatClientBuilder)
            .build();

    return rewriteQueryTransformer;
  }

  public String chatWithRewriteQuery(String question, double score, String source) {
    // RetrievalAugmentationAdvisor 생성
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = 
        RetrievalAugmentationAdvisor.builder()
            .queryTransformers(createRewriteQueryTransformer())
            .documentRetriever(createVectorStoreDocumentRetriever(score, source))
            .build();

    // 프롬프트를 LLM으로 전송하고 응답을 받는 코드
    String answer = this.chatClient.prompt()
        .user(question)
        .advisors(retrievalAugmentationAdvisor)
        .call()
        .content();
    return answer;
  }  


  //-------------------------------------------------------------------------------

  // ##### TranslationQueryTransformer 생성하고 반환하는 메소드 #####
  private TranslationQueryTransformer createTranslationQueryTransformer() {
    // 새로운 ChatClient를 생성하는 빌더 생성
    ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
        .defaultAdvisors(
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
        );

    // 질문 번역기 생성
    TranslationQueryTransformer translationQueryTransformer = 
        TranslationQueryTransformer.builder()
            .chatClientBuilder(chatClientBuilder)
            .targetLanguage("korean")
            .build();

    return translationQueryTransformer;
  }

  // ##### LLM과 대화하는 메소드 #####
  public String chatWithTranslation(String question, double score, String source) {
    // RetrievalAugmentationAdvisor 생성
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = 
        RetrievalAugmentationAdvisor.builder()
            .queryTransformers(createTranslationQueryTransformer())
            .documentRetriever(createVectorStoreDocumentRetriever(score, source))
            .build();

    // 프롬프트를 LLM으로 전송하고 응답을 받는 코드
    String answer = this.chatClient.prompt()
        .user(question)
        .advisors(retrievalAugmentationAdvisor)
        .call()
        .content();
    return answer;
  }

  //-------------------------------------------------------------------------------
  // ##### MultiQueryExpander 생성하고 반환하는 메소드 #####
  private MultiQueryExpander createMultiQueryExpander() {
    // 새로운 ChatClient 빌더 생성
    ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
        .defaultAdvisors(
            new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
        );

    // 질문 확장기 생성
    MultiQueryExpander multiQueryExpander = 
        MultiQueryExpander.builder()
            .chatClientBuilder(chatClientBuilder)
            .includeOriginal(true)
            .numberOfQueries(3)
            .build();

    return multiQueryExpander;
  }

  // ##### LLM과 대화하는 메소드 #####
  public String chatWithMultiQuery(String question, double score, String source) {
    // RetrievalAugmentationAdvisor 생성
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = 
        RetrievalAugmentationAdvisor.builder()
            .queryExpander(createMultiQueryExpander())
            .documentRetriever(createVectorStoreDocumentRetriever(score, source))
            .build();

    // 프롬프트를 LLM으로 전송하고 응답을 받는 코드
    String answer = this.chatClient.prompt()
        .user(question)
        .advisors(retrievalAugmentationAdvisor)
        .call()
        .content();
    return answer;
  }
}
