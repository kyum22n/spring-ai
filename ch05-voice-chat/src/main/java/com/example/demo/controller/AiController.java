package com.example.demo.controller;

import java.io.OutputStream;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.service.AiService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {
  // 주입
  @Autowired
  AiService aiService;
  
  // STT
  @PostMapping(
    value = "/stt",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.TEXT_PLAIN_VALUE
  ) public String stt(@RequestParam("speech") MultipartFile speech) throws Exception {
    
    String originalFileName = speech.getOriginalFilename();
    byte[] bytes = speech.getBytes();

    String text = aiService.stt(originalFileName, bytes);

    return text;
  }
  
  // TTS
  @PostMapping(
    value = "/tts",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
  ) public byte[] tts(@RequestParam("text") String text) {
    
    byte[] bytes = aiService.tts(text);

    return bytes;
  }

  // chat-text
  @PostMapping(
    value = "/chat-text",
    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  ) public Map<String, String> chatText(@RequestParam("question") String question) {
    Map<String, String> response = aiService.chatText(question);
    
    return response;
  }

  // 순수 음성 대화(STT-LLM-TTS)
  @PostMapping(
    value = "/chat-voice-stt-llm-tts",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
  ) 
  public void chatVoiceSttLlmTts(
    @RequestParam("question") MultipartFile question,
    HttpServletResponse response ) throws Exception {
    
    // 서버로부터 스트리밍으로 음성 데이터를 받아 리턴
    Flux<byte[]> flux = aiService.chatVoiceSttLlmTts(question.getBytes());

    // 출력 스트림
    OutputStream os = response.getOutputStream();

    // 스트림으로 계속 들어오는 바이트 배열 얻어내기
    for(byte[] chunk : flux.toIterable()) {
      os.write(chunk);
      os.flush();
    }
  
  }
  
  // 순수 음성 대화(gpt-4o-mini audio)
  @PostMapping(
    value = "/chat-voice-one-model",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
  ) 
  public byte[] chatVoiceOneModel(
    @RequestParam("question") MultipartFile question,
    HttpServletResponse response ) throws Exception {
    
    // 서버로부터 스트리밍으로 음성 데이터를 받아 리턴
    byte[] bytes = aiService.chatVoiceOneModel(question.getBytes(), question.getContentType());

    return bytes;
  }
}