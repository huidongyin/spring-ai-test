package io.github.huidong.yin.mcpclientsampling;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
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
					What is the weather in Amsterdam right now?
					Please incorporate all createive responses from all LLM providers.
					After the other providers add a poem that synthesizes the the poems from all the other providers.
					""";

            System.out.println("> USER: " + userQuestion);
            System.out.println("> ASSISTANT: " + chatClient.prompt(userQuestion).call().content());
        };
    }

    @Bean
    McpSyncClientCustomizer samplingCustomizer(Map<String, ChatClient> chatClients) {

        return (name, mcpClientSpec) -> {

            mcpClientSpec = mcpClientSpec.loggingConsumer(logingMessage -> {
                System.out.println("MCP LOGGING: [" + logingMessage.level() + "] " + logingMessage.data());
            });

            mcpClientSpec.sampling(llmRequest -> {
                var userPrompt = ((McpSchema.TextContent) llmRequest.messages().get(0).content()).text();
                String modelHint = llmRequest.modelPreferences().hints().get(0).name();

                ChatClient hintedChatClient = chatClients.entrySet().stream()
                        .filter(e -> e.getKey().contains(modelHint)).findFirst()
                        .orElseThrow().getValue();

                String response = hintedChatClient.prompt()
                        .system(llmRequest.systemPrompt())
                        .user(userPrompt)
                        .call()
                        .content();

                return McpSchema.CreateMessageResult.builder().content(new McpSchema.TextContent(response)).build();
            });
            System.out.println("Customizing " + name);
        };
    }

    @Bean
    public Map<String, ChatClient> chatClients(List<ChatModel> chatModels) {

        return chatModels.stream().collect(Collectors.toMap(model -> model.getClass().getSimpleName().toLowerCase(),
                model -> ChatClient.builder(model).build()));

    }
}
