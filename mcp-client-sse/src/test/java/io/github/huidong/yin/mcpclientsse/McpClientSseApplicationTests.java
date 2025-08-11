package io.github.huidong.yin.mcpclientsse;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

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


    // =======  API Test ===========//
    @Autowired
    private List<McpSyncClient> mcpSyncClients;  // For sync client

//    @Autowired
//    private List<McpAsyncClient> mcpAsyncClients;  // For async client

    @Test
    void mcpSyncClients() {
        for (McpSyncClient client : mcpSyncClients) {
            McpSchema.Implementation clientInfo = client.getClientInfo();
            log.info(">>> CLIENT_INFO: {}", clientInfo);
            McpSchema.Implementation serverInfo = client.getServerInfo();
            log.info(">>> SERVER_INFO: {}", serverInfo);
            McpSchema.ServerCapabilities serverCapabilities = client.getServerCapabilities();
            log.info(">>> SERVER_CAPABILITIES: {}", serverCapabilities);
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("getTimeZone", "{}"));
            log.info(">>> RESULT: {}", result);
        }
    }

    //vllm
    @Test
    void springAIIssueTest() {
        var chatClient = chatClientBuilder.defaultToolCallbacks(toolCallbackProvider).build();
        String question = "Tell me current time zone .";
        log.info(">>> QUESTION: {}", question);
        log.info(">>> ASSISTANT: {}", chatClient.prompt(question).stream().content().collectList().block());
    }



    //// =======  API Test ===========//

    @Autowired
    private SyncMcpToolCallbackProvider syncMcpToolCallbackProvider;

    @Test
    void syncMcpToolCallbacks() {
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();
            log.info(">>> TOOL_DEFINITION: {}", toolDefinition);
        }
    }


}
