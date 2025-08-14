package io.github.huidong.yin.mcpserverutility;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerUtilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerUtilityApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(WeatherService weatherService) {
        return ToolCallbackProvider.from(ToolCallbacks.from(weatherService));
    }
}
