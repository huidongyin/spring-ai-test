package io.github.huidong.yin.mcpclientutility;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
class McpClientUtilityApplicationTests {

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;
    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private List<McpSyncClient> mcpSyncClients;

    @Test
    void testToolCallbackAdapter() {
        for (McpSyncClient mcpSyncClient : mcpSyncClients) {
            McpSchema.ListToolsResult toolList = mcpSyncClient.listTools();
            for (McpSchema.Tool tool : toolList.tools()) {
                //系统提供了适配层，可以让 MCP 工具无缝地以 Spring AI 认可的接口形式运行，同时支持同步调用和异步调用，增强了灵活性和兼容性。
                ToolCallback toolCallback = new SyncMcpToolCallback(mcpSyncClient, tool);
                ToolDefinition toolDefinition = toolCallback.getToolDefinition();
                if (!toolDefinition.name().contains("Hello")) {
                    continue;
                }
                String result = toolCallback.call("{}");
                log.info("tool:{} , execute result : {} .", toolDefinition.name(), result);
            }
        }
    }

    @Test
    void McpToolCallbackProvider() {
        for (McpSyncClient mcpSyncClient : mcpSyncClients) {
            SyncMcpToolCallbackProvider callbackProvider = new SyncMcpToolCallbackProvider(mcpSyncClient);
            ToolCallback[] toolCallbacks = callbackProvider.getToolCallbacks();
            for (ToolCallback toolCallback : toolCallbacks) {
                String name = toolCallback.getToolDefinition().name();
                log.info("tool name : {}", name);
            }
        }

        log.info("====");
        List<ToolCallback> toolCallbacks = SyncMcpToolCallbackProvider.syncToolCallbacks(mcpSyncClients);
        for (ToolCallback toolCallback : toolCallbacks) {
            String name = toolCallback.getToolDefinition().name();
            log.info("tool name : {}", name);
        }
    }

    @Test
    void testSpringAIToolToMcpTool() {
        //把我们在 Spring AI 里定义的工具回调，自动转成符合 MCP 协议要求的工具规格，这样这些工具就能被 MCP 服务器识别和调用，实现两者的兼容和联动。
        List<McpServerFeatures.SyncToolSpecification> mathTools = McpToolUtils
                .toSyncToolSpecifications(ToolCallbacks.from(new MathTools()));
//        McpSyncServer mcpSyncServer = new McpSyncServer(null);
//        for (McpServerFeatures.SyncToolSpecification tool : mathTools) {
//            mcpSyncServer.addTool(tool);
//        }

    }

    @Test
    void testGetToolCallbackFromMcpClient() {
        //从 MCP 客户端获取ToolCallback
        List<ToolCallback> toolCallbacks = McpToolUtils.getToolCallbacksFromSyncClients(mcpSyncClients);
        for (ToolCallback toolCallback : toolCallbacks) {
            String name = toolCallback.getToolDefinition().name();
            log.info("tool name : {}", name);
        }
    }



}
