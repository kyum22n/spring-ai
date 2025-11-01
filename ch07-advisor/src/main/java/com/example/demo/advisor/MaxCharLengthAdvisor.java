package com.example.demo.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxCharLengthAdvisor implements CallAdvisor {

  // 필드
  private int order;
  private int maxCharLength = 300;
  public static final String MAX_CHAR_LENGTH = "maxCharLength";

  // 생성자
  public MaxCharLengthAdvisor(int order) {
    this.order = order;
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public int getOrder() {
    return order;
  }

  @Override
  public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
    
    // 전처리(o)
    // 매개값인 ChatClientRequest는 직접 수정 불가
    // Prompt를 추가해서 보강된 새 ChatClientRequest를 얻기
    ChatClientRequest mutatedRequest = augmentPrompt(chatClientRequest);

    // 다음 Advisor에 넘겨줌
    ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(mutatedRequest);

    // 후처리(x)

    return chatClientResponse;
  }

  // Prompt를 추가해서 보강된 새 ChatClientRequest를 얻는 메소드
  private ChatClientRequest augmentPrompt(ChatClientRequest chatClientRequest) {
    // 보강 내용
    String userText = this.maxCharLength + "자 이내로 답변해주세요.";
    // 공유 객체 조사
    Integer maxCharLength = (Integer) chatClientRequest.context().get(MAX_CHAR_LENGTH);
    if (maxCharLength != null) {
      userText = maxCharLength + "자 이내로 답변해주세요.";
    }
    
    String finalUserText = userText;

    // 클래스는 없지만 클래스와 메소드를 만드는 것이 람다식
    // 따라서 로컬변수인 userText를 그대로 사용할 수 없고, 
    // 새로 선언된 finalUserText에 userText의 값을 담아 사용할 수 있다.
    Prompt prevPrompt = chatClientRequest.prompt();
    // 보강된 새 Prompt
    Prompt newPrompt = prevPrompt.augmentUserMessage(userMessage -> {
      // 보강된 새 UserMessage를 리턴
      return UserMessage.builder()
        .text(userMessage.getText() + "\n" + finalUserText)
        .build();
    });

    // 새 ChatClientRequest 생성
    ChatClientRequest newChatClientRequest = chatClientRequest.mutate()
      .prompt(newPrompt)
      .build();

    return newChatClientRequest;
  }
}
