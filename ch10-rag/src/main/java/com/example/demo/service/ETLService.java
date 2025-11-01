package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.ai.reader.JsonMetadataGenerator;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ETLService {

  @Autowired
  private VectorStore vectorStore;
  
  // 업로드된 파일을 가지고 ETL 과정을 처리
  public String etlFormFile(String title, String author, MultipartFile attach) throws Exception {
    
    // E: 추출(텍스트를 Document로 생성)
    List<Document> documents = extractFormFile(attach);

    // 변환하기 전에 Metadata를 먼저 추가
    for (Document doc : documents) {
      Map<String, Object> metadata = doc.getMetadata();
      metadata.put("title", title);
      metadata.put("author", author);
      metadata.put("source", attach.getOriginalFilename());
    }
    
    // T: 변환(잘게 쪼개는 과정)
    List<Document> splitted_documents = transform(documents);

    // L: 적재(VectorStore에 저장)
    vectorStore.add(splitted_documents);

    return "%d개를 %d개로 쪼개어서 Vector Store에 저장함.".formatted(documents.size(), splitted_documents.size());
  }
  
  // 추출 
  private List<Document> extractFormFile(MultipartFile attach) throws Exception {
    
    // 파일 정보 얻기
    String fileName = attach.getOriginalFilename();
    String contentType = attach.getContentType();
    byte[] bytes = attach.getBytes();

    log.info("contentType: " + contentType);
    
    // Resource 객체 생성
    Resource resource = new ByteArrayResource(bytes);
    
    List<Document> documents = new ArrayList<>();
    
    if (contentType.equals("text/plain")) {
      DocumentReader reader = new TextReader(resource);
      documents = reader.read();
      
    } else if (contentType.equals("application/pdf")) {
      DocumentReader reader = new PagePdfDocumentReader(resource);
      documents = reader.read();
      
    } else if (contentType.contains("word")) {
      DocumentReader reader = new TikaDocumentReader(resource);
      documents = reader.read();
      
    }
    
    log.info("추출된 Document 수: {}", documents.size());
    
    return documents;
  }
  
  // 변환
  private List<Document> transform(List<Document> documents) {
    
    // 분할
    TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();
    List<Document> splitted_docs = tokenTextSplitter.apply(documents);
    
    return splitted_docs;
  }
  
  // 업로드된 파일을 가지고 ETL 과정을 처리 (HTML)
  public String etlFromHtml(String title, String author, String url) throws Exception {
    
    // url로부터 Resource 얻기
    Resource resource = new UrlResource(url);
    
    // 추출하기
    JsoupDocumentReader reader = new JsoupDocumentReader(
      resource,
      JsoupDocumentReaderConfig.builder()
      .charset("UTF-8")
      .selector("#content")
      .additionalMetadata(Map.of(
          "title", title,
          "author", author,
          "url", url
          ))
      .build()
    );

    // E
    List<Document> documents = reader.read();
    
    // T
    DocumentTransformer transformer = new TokenTextSplitter(
      200, 50, 0, 10000, false
    );
    List<Document> splitted_documents = transformer.apply(documents);
      
    // L
    vectorStore.add(splitted_documents);
      
    return "%d개를 %d개로 쪼개어서 Vector Store에 저장함.".formatted(documents.size(), splitted_documents.size());
  }

  // 업로드된 파일을 가지고 ETL 과정을 처리 (JSON)
  public String etlFromJson(String url) throws Exception {
    // url로부터 Resource 얻기
    Resource resource = new UrlResource(url);
    
    // 추출하기
    JsonReader jsonReader = new JsonReader(
      resource,
      jsonMap -> Map.of(
        "title", jsonMap.get("title"),
        "author", jsonMap.get("author"),
        "url", url
      ),
      "date", "content"
    );

    // E
    List<Document> documents = jsonReader.read();
    
    // T
    DocumentTransformer transformer = new TokenTextSplitter(
      200, 50, 0, 10000, false
    );
    List<Document> splitted_documents = transformer.apply(documents);
      
    // L
    vectorStore.add(splitted_documents);
      
    return "%d개를 %d개로 쪼개어서 Vector Store에 저장함.".formatted(documents.size(), splitted_documents.size());
  }
}
  