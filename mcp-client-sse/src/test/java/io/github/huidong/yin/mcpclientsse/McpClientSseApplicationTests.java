package io.github.huidong.yin.mcpclientsse;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class McpClientSseApplicationTests {

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;
    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void contextLoads() {
        var chatClient = chatClientBuilder.defaultToolCallbacks(toolCallbackProvider).build();
        String question = "Tell me the weather about (47.6062, -122.3321) and also give me a weather alert .";
        log.info(">>> QUESTION: {}", question);
        log.info(">>> ASSISTANT: {}", chatClient.prompt(question).call().content());
    }

}
