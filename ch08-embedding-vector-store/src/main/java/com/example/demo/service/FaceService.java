package com.example.demo.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.dto.FaceEmbedApiResponse;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FaceService {
  
  // 직접 SQL문으로 DB에 작성하는 방법
  @Autowired
  private JdbcTemplate jdbcTemplate;

  // 외부 API 받아오기
  private WebClient webClient;

  // 생성자
  public FaceService(WebClient.Builder webClientBuilder) {
    webClient = webClientBuilder.build();
  }

  // 얼굴에 대한 임베딩 벡터 얻기
  public float[] getFaceVector(MultipartFile mf) throws Exception {
    
    // 이미지 파일 정보 얻기
    String fileName = mf.getOriginalFilename();
    String contentType = mf.getContentType();
    byte[] bytes = mf.getBytes();

    // Resource 생성
    Resource resource = new ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return fileName;
      }
    };

    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", resource); //파일파트

    FaceEmbedApiResponse faceEmbedApiResponse = webClient.post()
      // 요청 URL
      .uri("http://localhost:50001/get-face-vector")
      // Body에 멀티파트 폼 넣기
      .body(BodyInserters.fromMultipartData(form))
      // REST API 호출
      .retrieve()
      // Body에 받을 DTO 
      .bodyToMono(FaceEmbedApiResponse.class)
      // 모든 응답이 도착할 때까지 여기서 대기
      .block();
    log.info("vector: {}", faceEmbedApiResponse.getVector());
    
    return faceEmbedApiResponse.getVector();
    
  }

  // 얼굴 임베딩 벡터 저장
  public void addFace(String personName, MultipartFile mf) throws Exception {
    // 얼굴 임베딩
    float[] vector = getFaceVector(mf);

    // Vector store에 저장
    // float[] -> String
    String strVector = Arrays.toString(vector).replace(" ", " ");
    
    // SQL문 작성
    String sql = "INSERT INTO face_vector_store (content, embedding) VALUES (?, ?::vector)";

    // SQL문 실행
    // INSERT, UPDATE, DELETE 문을 실행할 때: update() 메소드 사용
    // SELECT문을 실행할 때: query() 메소드 사용
    // 매개값으로 sql문, 그리고 ?로 표시된 매개변수 값을 넘겨줌
    jdbcTemplate.update(sql,personName, strVector);
  }

  public String findFace(MultipartFile mf) throws Exception {

    // 쿼리 이미지의 벡터 얻기
    float[] vector = getFaceVector(mf);
    // 벡터를 문자열로 변환
    String strVector = Arrays.toString(vector).replace(" ", " ");
    // SQL문으로 유사도를 직접 검색
    // <->: 유클리드 거리, <=>: 코사인 거리
    String sql = """
      SELECT content, (embedding <-> ?::vector) as similarity
      FROM face_vector_store
      ORDER BY embedding <-> ?::vector
      LIMIT 3
    """;

    // 여러 개를 SELECT할 때는 queryForList
    List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, strVector, strVector);
    for(Map<String, Object> map : list) {
      log.info("{} (L2 거리: {})", map.get("content"), map.get("similarity"));
    }
    
    Double similarity = (Double) list.get(0).get("similarity");
    if(similarity <= 0.3) {
      String personName = (String) list.get(0).get("content");
      return personName;
    } else {
      return "등록된 사람이 없습니다.";
    }


  }
}
