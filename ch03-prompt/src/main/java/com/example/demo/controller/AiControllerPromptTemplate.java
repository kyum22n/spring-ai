package com.example.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiServicePromptTemplate;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/ai")
@Slf4j
public class AiControllerPromptTemplate {

  @Autowired
  private AiServicePromptTemplate aiService;

  @PostMapping(
    value = "/prompt-template",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_NDJSON_VALUE
  )
  public Flux<String> promptTemplate(
    @RequestParam("language") String language,
    @RequestParam("statement") String statement
  ) {
    // Flux<String> answer = aiService.promptTemplate1(language, statement);
    // Flux<String> answer = aiService.promptTemplate2(language, statement);
    // Flux<String> answer = aiService.promptTemplate3(language, statement);
    Flux<String> answer = aiService.promptTemplate4(language, statement);
    return answer;
  }
  
}
