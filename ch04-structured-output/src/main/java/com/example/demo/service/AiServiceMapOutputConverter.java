package com.example.demo.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiServiceMapOutputConverter {
  private ChatClient chatClient;

  public AiServiceMapOutputConverter(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // 저수준
  public Map<String, Object> mapOutputConverterLowLevel(String hotel) {
    
    MapOutputConverter mapOutputConverter = new MapOutputConverter();

    PromptTemplate promptTemplate = PromptTemplate.builder()
      .template("호텔 {hotel}에 대한 정보를 다음 형식으로 알려주세요.\n{format}")
      .build();


    String json = chatClient.prompt()
      .user(promptTemplate.render(Map.of(
        "hotel", hotel,
        "format", mapOutputConverter.getFormat()
      )))
      .call()
      .content();

    Map<String, Object> map = mapOutputConverter.convert(json);
    
    return map;
  }

  // 고수준
  public Map<String, Object> mapOutputConverterHighLevel(String hotel) {
    
    Map<String, Object> map = chatClient.prompt()
      .user("호텔 %s에 대한 정보를 알려주세요.".formatted(hotel))
      .call()
      .entity(new MapOutputConverter());  //converter.getFormat() + converter.convert()
    
    return map;
  }
}
