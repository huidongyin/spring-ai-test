package io.github.huidong.yin.mcpclientwebflux;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class McpClientWebfluxApplicationTests {

	@Autowired
	private ToolCallbackProvider toolCallbackProvider;
	@Autowired
	private ChatClient.Builder chatClientBuilder;

	@Test
	void contextLoads() {
		var chatClient = chatClientBuilder.defaultToolCallbacks(toolCallbackProvider).build();
		String question = "What tools do we have at our disposal?";
		log.info(">>> QUESTION: {}", question);
		log.info(">>> ASSISTANT: {}", chatClient.prompt(question).call().content());
	}

}
