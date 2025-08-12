package io.github.huidong.yin.mcpclientsampling;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootApplication
@Slf4j
public class McpClientSamplingApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpClientSamplingApplication.class, args);
    }

    @Bean
    public CommandLineRunner predefinedQuestions(OpenAiChatModel openAiChatModel,
                                                 List<McpSyncClient> mcpClients) {

        return args -> {

            var mcpToolProvider = new SyncMcpToolCallbackProvider(mcpClients);

            ChatClient chatClient = ChatClient.builder(openAiChatModel).defaultToolCallbacks(mcpToolProvider).build();

            String userQuestion = """
                    What is the weather in Los Angeles(latitude=34.0522,longitude=-118.2437) right now?
                    Please incorporate all createive responses from all LLM providers.
                    After the other providers add a poem that synthesizes the the poems from all the other providers.
                    """;

            log.info("> > > USER: {}", userQuestion);
            log.info("> > > ASSISTANT: {}", chatClient.prompt(userQuestion).call().content());
        };
    }

    @Bean
    McpSyncClientCustomizer samplingCustomizer(Map<String, ChatClient> chatClients) {

        return (name, mcpClientSpec) -> {

            mcpClientSpec = mcpClientSpec.loggingConsumer(logingMessage -> {
                log.info("MCP LOGGING: level: {} ,data: {}", logingMessage.level(), logingMessage.data());
            });

            mcpClientSpec.sampling(llmRequest -> {
                var userPrompt = ((McpSchema.TextContent) llmRequest.messages().get(0).content()).text();
                String modelHint = llmRequest.modelPreferences().hints().get(0).name();
                log.info("MCP LOGGING: [ start process mcp server request ] {}", modelHint);
                ChatClient hintedChatClient = chatClients.entrySet().stream()
                        .filter(e -> e.getKey().contains(modelHint)).findFirst()
                        .orElseThrow().getValue();

                String response = hintedChatClient.prompt()
                        .system(llmRequest.systemPrompt())
                        .user(userPrompt)
                        .call()
                        .content();
                log.info("MCP LOGGING: [ end process mcp server request ] {}", modelHint);
                return McpSchema.CreateMessageResult.builder().content(new McpSchema.TextContent(response)).build();
            });
            log.info("Customizing {}", name);
        };
    }

    @Bean
    public Map<String, ChatClient> chatClients(List<ChatModel> chatModels) {

        return chatModels.stream().collect(Collectors.toMap(model -> model.getClass().getSimpleName().toLowerCase(),
                model -> ChatClient.builder(model).build()));

    }
}
