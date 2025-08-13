package io.github.huidong.yin.mcpclientdynamictoolupdate;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest
@Slf4j
class McpClientDynamicToolUpdateApplicationTests {

    @Autowired
    private ChatModel deepseekChatModel;

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    @Autowired
    private List<McpSyncClient> mcpSyncClients;

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

    public record ToolDescription(String toolName, String toolDescription) {
        @Override
        public String toString() {
            return "Tool: " + toolName + " -> " + toolDescription;
        }
    }


    @Test
    public void test() {
        for (McpSyncClient client : mcpSyncClients) {
            McpSchema.Implementation serverInfo = client.getServerInfo();
            String name = serverInfo.name();
            String version = serverInfo.version();
            log.info("current mcp sync client : {} , server info : [name:{} , version:{}]",client.getClientInfo().name(),name,version);
            McpSchema.ListToolsResult tools = client.listTools();
            String toolList = tools.tools().stream().map(McpSchema.Tool::name).collect(Collectors.joining(","));
            log.info("tool list : \n {}", toolList);
        }
    }

    @Test
    public void test2() {
        log.info("tool callback provider type : {}", toolCallbackProvider instanceof SyncMcpToolCallbackProvider ? "Sync":"Async");
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        String tools = Arrays.stream(toolCallbacks).map(toolCallback -> toolCallback.getToolDefinition().name()).collect(Collectors.joining(","));
        log.info("tool list : \n {}", tools);
    }

}
