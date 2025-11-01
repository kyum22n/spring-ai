package com.example.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AiServiceMultiMessages;
import com.example.demo.service.AiServicePromptTemplate;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/ai")
@Slf4j
public class AiControllerMultiMessages {

  @Autowired
  private AiServiceMultiMessages aiService;

  @PostMapping(
    value = "/multi-messages",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String promptTemplate(
    @RequestParam("question") String question,
    HttpSession session
  ) {
    // 세션에서 대화 기억 가져오기
    List<Message> chatMemory = (List<Message>) session.getAttribute("chatMemory");
    if (chatMemory == null) {
      chatMemory = new ArrayList<>();
      session.setAttribute("chatMemory", chatMemory);
    }

    String answer = aiService.multiMessages(question, chatMemory);

    return answer;
  }
  
}
