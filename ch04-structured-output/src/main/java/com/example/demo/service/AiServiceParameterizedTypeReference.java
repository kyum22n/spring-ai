package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.example.demo.dto.Hotel;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiServiceParameterizedTypeReference {
  private ChatClient chatClient;

  public AiServiceParameterizedTypeReference(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  // 저수준
  public List<Hotel> genericBeanOutputConverterLowLevel(String cities) {
    BeanOutputConverter<List<Hotel>> converter = new BeanOutputConverter<>(
        new ParameterizedTypeReference<List<Hotel>>() {
        });

    PromptTemplate promptTemplate = PromptTemplate.builder()
        .template("""
            다음 도시들에서 유명한 호텔 3개를 형식에 맞게 출력하시오.
            도시: {cities}
            형식: {format}
            """)
        .build();

    String json = chatClient.prompt()
        .user(promptTemplate.render(Map.of("cities", cities, "format", converter.getFormat())))
        .call()
        .content();

    List<Hotel> listHotel = converter.convert(json);

    return listHotel;
  }

  // 고수준
  public List<Hotel> genericBeanOutputConverterHighLevel(String cities) {
    List<Hotel> listHotel = chatClient.prompt()
        .user("다음 도시들에서 유명한 호텔 3개를 출력하시오.\n%s".formatted(cities))
        .call()
        .entity(new ParameterizedTypeReference<List<Hotel>>() {
        });

    return listHotel;
  }

}
