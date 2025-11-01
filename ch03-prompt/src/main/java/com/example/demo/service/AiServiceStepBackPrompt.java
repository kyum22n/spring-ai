package com.example.demo.service;

import java.util.List;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiServiceStepBackPrompt {
  private ChatClient chatClient;

  public AiServiceStepBackPrompt(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String stepBackPrompt2(String question) throws Exception {
    String answer = chatClient.prompt()
      .user(question)
      .call()
      .content();
    return answer;
  }

  public String stepBackPrompt(String question) throws Exception {
    String questions = chatClient.prompt()
      .user("""
          사용자 질문을 처리하기 Step-Back 프롬프트 기법을 사용하려고 합니다.
          사용자 질문을 단계별 질문들로 재구성해주세요. 
          맨 마지막 질문은 사용자 질문과 일치해야 합니다.
          단계별 질문을 항목으로 하는 JSON 배열로 출력해 주세요.
          예시: ["...", "...", "...", ...]
          사용자 질문: %s
        """.formatted(question))
      .call()
      .content();

    // ["...", "...", "..."]을 추출하기
    String json = questions.substring(
      questions.indexOf("["),
      questions.indexOf("]") + 1
    );

    // ["...", "...", "..."] -> List<String>
    ObjectMapper objectMapper = new ObjectMapper(); //JSON을 Java 객체로 매핑

    List<String> listQuestion = objectMapper.readValue(
      json, 
      // 익명 자식 객체로 만들어 상속
      // List가 제네릭 타입이므로 TypeReference 상속
      new TypeReference<List<String>>() {}
    );

    log.info(listQuestion.toString());

    // 단계별 질문에 대한 답변을 얻고 다음 단계 질문에 포함시키기
    String[] answerArray = new String[listQuestion.size()];
    for (int i=0; i<listQuestion.size(); i++) {
      String stepQuestion = listQuestion.get(i);
      String stepAnswer = getStepAnswer(stepQuestion, answerArray);
      answerArray[i] = stepAnswer;
    }

    String finalAnswer = answerArray[answerArray.length -1];
    return finalAnswer;
  }

  // 단계별 답변
  public String getStepAnswer(String question, String[] answerArray) {
    // 이전 답변글을 모두 context로 누적
    String context = "";
    for (String answer : answerArray) {
      // if (answer != null) {
      //   context += answer;
      // }

      context += Objects.requireNonNullElse(answer,"");
    }

    // 현재 단계 질문에 대해 답변 얻기
    String answer = chatClient.prompt()
      .user("""
          다음 question에 대한 답변을 context 기반으로 답변해 주세요.
          context가 없다면 인터넷을 서칭해서 답변을 해주세요.
          question: %s
          context: %s    
        """.formatted(question, context)
      )  
      .call()
      .content();

    return answer;
  }
}
