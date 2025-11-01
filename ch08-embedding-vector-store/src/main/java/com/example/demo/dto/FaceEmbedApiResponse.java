package com.example.demo.dto;

import lombok.Data;

// class를 생성해 필드에 vector를 저장
@Data
public class FaceEmbedApiResponse {
  private float[] vector;
}
