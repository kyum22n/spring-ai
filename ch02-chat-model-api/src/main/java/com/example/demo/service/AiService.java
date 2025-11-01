package com.example.demo.service;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AiService {
  @Autowired
  private ChatModel chatModel; // 기본적으로 gpt-4o-mini

  public String generateText(String question) {
    // 시스템 메시지 생성
    // LLM에게 지시하는 내용
    SystemMessage systemMessage = SystemMessage.builder()
        .text("사용자의 질문에 대해 한글로 답변하세요")
        .build();

    // 사용자 메시지 생성
    // 메소드 체이닝 기법
    UserMessage userMessage = UserMessage.builder()
        .text(question)
        .build();
    /*
     * Builder builder = UserMessage.builder();
     * builder.text(question);
     * UserMessage userMessage = builder.build();
     */

    // 대화옵션
    ChatOptions chatOptions = ChatOptions.builder()
        .model("gpt-4o-mini")
        // .maxTokens(100)
        .temperature(1.0)
        .build();

    // 프롬프트 생성
    Prompt prompt = Prompt.builder()
        .messages(systemMessage, userMessage)
        .chatOptions(chatOptions)
        .build();

    // LLM에게 요청하고 응답받기
    ChatResponse chatResponse = chatModel.call(prompt);
    log.info("-------------------------");
    log.info(chatResponse.toString());
    log.info("-------------------------");

    AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
    String answer = assistantMessage.getText();
    log.info("answer" + answer);
    log.info("사용 토큰 수: " + chatResponse.getMetadata().getUsage().getTotalTokens());

    return answer;
  }

  public Flux<String> generateStreamText(String question) {
    // 시스템 메시지 생성
    SystemMessage systemMessage = SystemMessage.builder()
        .text("사용자의 질문에 대해 한글로 답변하세요")
        .build();

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        .text(question)
        .build();

    // 대화옵션
    ChatOptions chatOptions = ChatOptions.builder()
        .model("gpt-4o-mini")
        // .maxTokens(100)
        .temperature(1.0)
        .build();

    // 프롬프트 생성
    Prompt prompt = Prompt.builder()
        .messages(systemMessage, userMessage)
        .chatOptions(chatOptions)
        .build();

    // LLM에게 요청하고 응답받기
    // ChatResponse가 String으로 계속 리턴됨
    // ChatResponse로 흘러오는 응답 객체를 map을 이용해 String으로 변환
    Flux<ChatResponse> fluxChatResponse = chatModel.stream(prompt);
    Flux<String> fluxString = fluxChatResponse.map(chatResponse -> {
      AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
      String str = assistantMessage.getText();
      return str;
    });

    // Flux<String> fluxString = chatModel.stream(systemMessage, userMessage);

    return fluxString;
  }
}
