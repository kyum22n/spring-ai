package com.example.demo;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.demo.tool.DateTimeTools;

@SpringBootApplication
public class DemoApplication {

	// 외부 도구 주입
	@Autowired
	private DateTimeTools dateTimeTools;

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	// MCP Server -> MCP Client 도구 정보 제공
	@Bean
	public ToolCallbackProvider getToolCallbackProvider() {
		return MethodToolCallbackProvider.builder()
				.toolObjects(dateTimeTools)
				.build();
	}

}
