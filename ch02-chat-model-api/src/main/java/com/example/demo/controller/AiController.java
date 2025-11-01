package com.example.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiService;
import com.example.demo.service.AiServiceByChatClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {
  // @Autowired
  // private AiService aiService;

  @Autowired
  private AiServiceByChatClient aiService;

  @PostMapping(
    value = "/chat-model",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String chatModel(@RequestParam("question") String question) {
    String answer = aiService.generateText(question);
    return answer;
  }
  
  @PostMapping(
    value = "/chat-model-stream",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_NDJSON_VALUE
  )
  public Flux<String> chatModelStream(@RequestParam("question") String question) {
    Flux<String> answer = aiService.generateStreamText(question);
    return answer;
  }
  
}
