package com.example.demo.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionFinishReason;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.AudioParameters;
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AiService {
  // ChatClient, TTS/STT 얻기
  private ChatClient chatClient;
  private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;  //STT
  private OpenAiAudioSpeechModel openAiAudioSpeechModel;  //TTS

  // 생성자 주입
  public AiService(
    ChatClient.Builder chatClientBuilder,
    OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel,
    OpenAiAudioSpeechModel openAiAudioSpeechModel
  ) {
    this.chatClient = chatClientBuilder.build();
    this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
    this.openAiAudioSpeechModel = openAiAudioSpeechModel;
  }

  // STT(Speech-To-Text)
  public String stt(String fileName, byte[] bytes) {
    
    // 음성 데이터를 ByteArrayResource로 생성
    // Spring AI 1.0.3에서는 파일 이름이 필수로 추가되어야 함
    Resource audioResource = new ByteArrayResource(bytes) {
      // 파일 이름&포맷 전달
      @Override
      public String getFilename() {
        return fileName;
      }
    };

    // AudioTranscriptionOptions 생성하여 옵션 지정, 힌트 제공
    AudioTranscriptionOptions audioTranscriptionOptions = OpenAiAudioTranscriptionOptions.builder()
      .model("whisper-1")
      .language("ko")
      .build();

    // Prompt 생성
    AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, audioTranscriptionOptions);

    // 모델 호출 및 응답
    AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);
    String text = response.getResult().getOutput();

    return text;
  }

  // TTS(Text-To-Speech)
  public byte[] tts(String text) {

    // Options
    OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
      .model("gpt-4o-mini-tts")
      .voice(SpeechRequest.Voice.ALLOY)
      .speed(1.0f)
      .responseFormat(SpeechRequest.AudioResponseFormat.MP3)
      .build();

    // Prompt 생성
    SpeechPrompt speechPrompt = new SpeechPrompt(text, speechOptions);

    SpeechResponse speechResponse = openAiAudioSpeechModel.call(speechPrompt);
    byte[] bytes = speechResponse.getResult().getOutput();

    return bytes;
  }
  
  // chat-text
  public Map<String, String> chatText(String question) {
    
    // LLM 사용
    String textAnswer = chatClient.prompt()
    .system("50자 이내의 한국어로 답변해주세요.")
    .user(question)
    .call()
    .content();
    
    // TTS 사용
    byte[] audio = tts(textAnswer); // 위에서 만듦
    
    // base64로 변환
    String base64Audio = Base64.getEncoder().encodeToString(audio);
    
    // Map에 답기
    Map<String, String> map = new HashMap<>();
    map.put("text", textAnswer);
    map.put("audio", base64Audio);
    
    
    return map;
  }
  
  // TTS(Flux)
  public Flux<byte[]> ttsFlux(String text) {

    // Options
    OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
      .model("gpt-4o-mini-tts")
      .voice(SpeechRequest.Voice.ALLOY)
      .speed(1.0f)
      .responseFormat(SpeechRequest.AudioResponseFormat.MP3)
      .build();

    // Prompt 생성
    SpeechPrompt speechPrompt = new SpeechPrompt(text, speechOptions);

    // 모델 호출 및 응답
    Flux<SpeechResponse> fluxSpeechResponse = openAiAudioSpeechModel.stream(speechPrompt);
    Flux<byte[]> fluxBytes = fluxSpeechResponse.map(speechResponse -> speechResponse.getResult().getOutput());
    return fluxBytes;
  }

  // 순수 음성 대화 (방법1)
  public Flux<byte[]> chatVoiceSttLlmTts(byte[] audioBytes) {

    // 음성->STT->텍스트
    String textQuestion = stt("speech.mp3", audioBytes);
    
    // 텍스트->LLM->텍스트
    String textAnswer = chatClient.prompt()
    .system("50자 이내로 답변해주세요.")
    .user(textQuestion)
    .call()
    .content();
    
    // 텍스트->TTS->음성
    Flux<byte[]> flux = ttsFlux(textAnswer);
    return flux;

  }

  // 순수 음성 대화 (방법2)
  public byte[] chatVoiceOneModel(byte[] audioBytes, String contentType) throws Exception {
    
    // Resource 생성
    Resource resource = new ByteArrayResource(audioBytes) {
      @Override
      public String getFilename() {
        return "speech.mp3";
      }
    };

    // UserMessage 생성 (음성 포함)
    UserMessage userMessage = UserMessage.builder()
      .text("제공되는 음성에 맞는 자연스러운 대화로 이어주세요.") // 지시문에 가까움
      .media(new Media(MimeType.valueOf(contentType), resource))
      .build();

    // Options
    ChatOptions chatOptions = OpenAiChatOptions.builder()
      // 모델 지정
      .model(OpenAiApi.ChatModel.GPT_4_O_MINI_AUDIO_PREVIEW)
      // 텍스트와 음성 반환
      .outputModalities(List.of("text", "audio"))
      .outputAudio(new AudioParameters(
        // 오디오 음성
        ChatCompletionRequest.AudioParameters.Voice.ALLOY,
        // 인코딩 타입
        ChatCompletionRequest.AudioParameters.AudioResponseFormat.MP3
      ))
      .build();

    // 모델 호출 및 응답
    ChatResponse response = chatClient.prompt()
      .system("50자 이내로 답변해주세요.")
      .messages(userMessage)
      .options(chatOptions)
      .call()
      .chatResponse();  // 동기

    // AssistantMessage 생성
    AssistantMessage assistantMessage = response.getResult().getOutput();
    // 텍스트 데이터 얻기
    String textAnswer = assistantMessage.getText();
    log.info("텍스트 응답: " + textAnswer);
    // 음성 데이터 얻기
    byte[] audioAnswer = assistantMessage.getMedia().get(0).getDataAsByteArray();

    return audioAnswer;
  }
}
