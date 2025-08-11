package io.github.huidong.yin.mcpclientdynamictoolupdate;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class McpCustomerConfig {

    public static final AtomicBoolean flag = new AtomicBoolean(false);

    AtomicInteger atomicInteger = new AtomicInteger(0);

    @Bean
    public McpSyncClientCustomizer mcpSyncClientCustomizer() {
        return new McpSyncClientCustomizer() {
            @Override
            public void customize(String name, McpClient.SyncSpec spec) {
                spec.toolsChangeConsumer(new Consumer<List<McpSchema.Tool>>() {
                    @Override
                    public void accept(List<McpSchema.Tool> tools) {
                        log.info("times:{}tools change: {}", atomicInteger.incrementAndGet(), tools);
                        flag.set(true);
                    }
                });
            }
        };
    }
}
