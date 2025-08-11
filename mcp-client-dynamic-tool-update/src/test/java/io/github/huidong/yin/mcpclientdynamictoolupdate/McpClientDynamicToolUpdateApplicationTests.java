package io.github.huidong.yin.mcpclientdynamictoolupdate;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SpringBootTest
@Slf4j
class McpClientDynamicToolUpdateApplicationTests {



    @Autowired
    private ChatModel deepseekChatModel;

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    @Test
    void contextLoads() {
        ChatClient chatClient = ChatClient.builder(deepseekChatModel).build();
        List<ToolDescription> toolDescriptionList = chatClient.prompt("What tools are available? Please list them and avoid any additional comments. Only JSON format.")
                .toolCallbacks(toolCallbackProvider)
                .call()
                .entity(new ParameterizedTypeReference<List<ToolDescription>>() {
                });
        log.info("tool list : \n {}", (CollectionUtils.isEmpty(toolDescriptionList) ? Collections.emptyList() : toolDescriptionList).stream().map(td -> td.toString()).collect(Collectors.joining("\n")));

        // signal the server to update the tools
        String signal = RestClient.builder().build().get()
                .uri("http://localhost:8080/updateTools").retrieve().body(String.class);
        log.info("Server tool update response: {}", signal);
        while (!McpCustomerConfig.flag.get()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        chatClient = ChatClient.builder(deepseekChatModel).build();
        toolDescriptionList = chatClient.prompt("What tools are available? Please list them and avoid any additional comments. Only JSON format.")
                .toolCallbacks(toolCallbackProvider)
                .call()
                .entity(new ParameterizedTypeReference<List<ToolDescription>>() {
                });
        log.info("tool list : \n {}", (CollectionUtils.isEmpty(toolDescriptionList) ? Collections.emptyList() : toolDescriptionList).stream().map(td -> td.toString()).collect(Collectors.joining("\n")));
    }

    public static record ToolDescription(String toolName, String toolDescription) {
        @Override
        public final String toString() {
            return "Tool: " + toolName + " -> " + toolDescription;
        }
    }

}
