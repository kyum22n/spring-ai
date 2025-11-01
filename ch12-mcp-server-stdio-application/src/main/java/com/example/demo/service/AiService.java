package com.example.demo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiService {
  // ##### 필드 #####
  private ChatClient chatClient;

  // ##### 생성자 #####
  public AiService(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ToolCallbackProvider toolCallbackProvider) {
   this.chatClient = chatClientBuilder
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
      // 프롬프트에 MCP의 모든 외부 도구에 대한 정보들을 추가
      .defaultToolCallbacks(toolCallbackProvider)
      .build();
  } 

  // ##### LLM과 텍스트로 대화하는 메소드 #####
  public String chat(String question, String conversationId) {
    String answer = this.chatClient.prompt()
        .system("""
            현재 날짜와 시간 질문은 반드시 도구를 사용하세요.
            파일과 디렉토리 관련 질문은 반드시 도구를 사용하세요.
            """)
        .user(question)
        .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
        .call()
        .content();
    return answer;
  }

  // ##### 사진에서 차량 번호판을 인식하고 차단 봉을 제어하는 메소드 #####
  public String boomBarrier(String contentType, byte[] bytes) {
    // 미디어 생성
    Media media = Media.builder()
        .mimeType(MimeType.valueOf(contentType))
        .data(new ByteArrayResource(bytes))
        .build();

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        .text("""
            이미지에서 '(숫자 2개~3개)-(한글 1자)-(숫자 4개)'로 구성된 
            차량 번호를 인식하세요. 예: 78라1234, 567바2558
            인식된 차량 번호가 등록된 차량 번호인지 도구로 확인을 하세요.
            등록된 번호라면 도구로 차단 봉을 올리고, 답변은 '차단기 올림'으로 하세요.
            등록된 번호가 아니라면 도구로 차단 봉을 내리고, 답변은 '차단기 내림'으로 하세요.
        """)
        .media(media)
        .build();

    // LLM으로 요청하고 응답받기
    String answer = chatClient.prompt()
        .messages(userMessage)
        .call()
        .content();
    return answer;
  }
}
