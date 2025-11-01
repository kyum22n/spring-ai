package com.example.demo.internetsearch;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class InternetSearchTools {
  // ##### 필드 #####
  private String searchEndpoint;
  private String apiKey;
  // 검색 API에서 어떤 
  private String engineId;
  // 외부 REST API 호출하기 위함
  private WebClient webClient;
  // JSON을 Java 객체로 파싱
  private ObjectMapper objectMapper = new ObjectMapper();

  // ##### 생성자 #####
  public InternetSearchTools(
      // application.properties에 선언된 환경변수 읽어서 주입
      @Value("${google.search.endpoint}") String endpoint,      
      @Value("${google.search.apiKey}") String apiKey,      
      @Value("${google.search.engineId}") String engineId,
      WebClient.Builder webClientBuilder
  ) {
    this.searchEndpoint = endpoint;
    this.apiKey = apiKey;
    this.engineId = engineId;
    this.webClient = webClientBuilder
        .baseUrl(searchEndpoint)
        .defaultHeader("Accept", "application/json")
        .build();
  }

  // ##### 도구 #####
  @Tool(description = "인터넷 검색을 합니다. 제목, 링크, 요약을 문자열로 반환합니다.")
  public String googleSearch(String query) {
    try {
      String responseBody = webClient.get()
          .uri(uriBuilder -> uriBuilder
              .queryParam("key", apiKey)
              .queryParam("cx", engineId)
              .queryParam("q", query)
              .build())
          .retrieve()
          .bodyToMono(String.class)
          .block();
      //log.info("응답본문: {}", responseBody);

      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode items = root.path("items");

      if (!items.isArray() || items.isEmpty()) {
        return "검색 결과가 없습니다.";
      }

      StringBuilder sb = new StringBuilder();
      // item의 값을 하나씩 가져와 문자열을 만듦
      for (int i = 0; i < Math.min(3, items.size()); i++) {
        JsonNode item = items.get(i);
        String title = item.path("title").asText();
        String link = item.path("link").asText();
        String snippet = item.path("snippet").asText();
        // "...".formatted(i+1, title, link, snippet)으로 작성해도 됨
        sb.append(String.format("[%d] %s\n%s\n%s\n\n", i + 1, title, link, snippet));
      }
      return sb.toString().trim();

    } catch (Exception e) {
      return "인터넷 검색 중 오류 발생: " + e.getMessage();
    }
  }

  @Tool(description = "웹 페이지의 본문 텍스트를 반환합니다.")
  public String fetchPageContent(String url) {
    try {
      // WebClient를 사용해 응답 HTML 가져오기
      String html = webClient.get()
          .uri(url)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      if (html == null || html.isBlank()) {
        return "페이지 내용을 가져올 수 없습니다.";
      }

      // Jsoup으로 파싱하고 <body> 내부 텍스트 추출
      Document doc = Jsoup.parse(html);
      String bodyText = doc.body().text();

      return bodyText.isBlank() ? "본문 텍스트가 비어 있습니다." : bodyText;

    } catch (Exception e) {
      return "페이지 로딩 중 오류 발생: " + e.getMessage();
    }
  }
}
