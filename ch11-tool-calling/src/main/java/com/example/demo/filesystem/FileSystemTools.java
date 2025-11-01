package com.example.demo.filesystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileSystemTools {
  private final Path rootDirectory;

  public FileSystemTools() {
    Path home = Paths.get(System.getProperty("user.home"));
    this.rootDirectory = home.resolve("Documents/ch11-tool-calling");
    try {
      Files.createDirectories(rootDirectory);
    } catch (IOException e) {
      throw new RuntimeException("루트 디렉토리 생성 실패: " + rootDirectory, e);
    }
  }

  private Path resolve(String relativePath) {
    Path path = null;
    if (!StringUtils.hasText(relativePath)) {
      path = rootDirectory;
    }
    
    path = rootDirectory.resolve(relativePath).normalize();

    if (!path.startsWith(rootDirectory)) {
      path = rootDirectory;
    }
    
    return path;
  }
  
  @Tool(description = "디렉토리 항목 조회")
  public List<Item> listFiles(String relativePath) {
    // LLM이 받은 상대경로를 절대 경로로 바꿈
    Path path = resolve(relativePath);
    try {
      // 내부 반복자 얻음
      Stream<Path> stream = Files.list(path);
      // Path 객체를 Item 객체로 변환해서 리스트로 반환
      List<Item> list = stream.map(p -> {
        return new Item(p, Files.isDirectory(p));
      })
      .toList();
      stream.close();
      return list;
    } catch(Exception e) {
      log.info(e.toString());
      return new ArrayList<>();
    }
  }

  @Data
  @AllArgsConstructor
  public class Item {
    private Path path;
    private boolean directory;
  }

  @Tool(description = "디렉토리 생성")
  public String createDir(String relativePath) {
    Path path = resolve(relativePath);
    try {
      Files.createDirectories(path);
      return "디렉토리를 생성했습니다.";
    } catch(Exception e) {
      log.info(e.toString());
      return "디렉토리를 생성할 수 없습니다.";
    }
  }

  @Tool(description = "파일 생성")
  public String createFile(
    @ToolParam(description = "부모 디렉토리") String parentPath, 
    @ToolParam(description = "파일 이름") String fileName, 
    @ToolParam(description = "확장 이름") String extName, 
    @ToolParam(description = "파일 내용") String content
  ) {
    if(!StringUtils.hasText(fileName) ||
      !StringUtils.hasText(extName)) {
      return "디렉토리 또는 파일명이 없습니다.";
    }
    if(!StringUtils.hasText(content)) {
      content = "";
    }
    // parentPath로부터 상대 경로 얻기
    Path path = resolve(parentPath);
    // 파일 이름과 확장 이름이 분리되어 있을 경우 합치기
    if(!fileName.endsWith("." + extName)) {
      path = path.resolve(fileName + "." + extName);
    } else {
      path = path.resolve(fileName);
    }
    try {
      // OutputStream으로 처리해도 ㄱㅊ
      Files.writeString(path, content, StandardCharsets.UTF_8);
      return "파일을 생성했습니다.";
    } catch(Exception e) {
      log.info(e.toString());
      return "파일 생성에 실패했습니다.";
    }
  }

  @Tool(description = "파일 내용 읽기")
  public String readFile(String relativePath) {
    Path path = resolve(relativePath);
    if (Files.notExists(path)) {
      log.info(path.toString());
      return "파일이 존재하지 않습니다.";
    }
    try {
      String content = Files.readString(path, StandardCharsets.UTF_8);
      return content;
    } catch(Exception e) {
      log.info(e.toString());
      return "파일 내용을 읽을 수 없습니다.";
    }
  }

  @Tool(description = "파일 및 디렉토리 삭제")
  public String deletePath(String relativePath) {
    Path path = resolve(relativePath);
    if (Files.notExists(path))
      return "파일 또는 디렉토리가 존재하지 않습니다.";
    try {
      Stream<Path> stream = Files.walk(path);
      stream
        .sorted(Comparator.reverseOrder()) // 자식 → 부모 순서로 삭제
        .forEach(p -> {
          try {
            Files.delete(p);
          } catch (IOException ignored) {
          }
        });
      stream.close();
      return "파일 또는 디렉토리를 삭제했습니다.";
    } catch(Exception e) {
      log.info(e.toString());
      return "파일 또는 디렉토리를 삭제하지 못했습니다.";
    }
  }

  @Tool(description = "파일 이동 또는 이름 변경")
  public String moveFile(String sourceRelativePath, String targetRelativePath) {
    Path source = resolve(sourceRelativePath);
    Path target = resolve(targetRelativePath);
    try {
      // 위치에 동일한 파일이 있으면 덮어쓰기
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
      return "파일 이동 또는 이름 변경을 했습니다.";
    } catch(Exception e) {
      log.info(e.toString());
      return "파일 이동 또는 이름 변경을 못했습니다.";
    }
  }

}
