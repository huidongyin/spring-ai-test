package io.github.huidong.yin.mcpserversampling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WeatherService {


    private static final String BASE_URL = "https://api.weather.gov";

    private final RestClient restClient;

    public WeatherService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", "application/geo+json")
                .defaultHeader("User-Agent", "WeatherApiClient/1.0 huidong.yin247203@gmail.com")
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Points(@JsonProperty("properties") Props properties) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Props(@JsonProperty("forecast") String forecast) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Forecast(@JsonProperty("properties") Props properties) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Props(@JsonProperty("periods") List<Period> periods) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Period(@JsonProperty("number") Integer number, @JsonProperty("name") String name,
                             @JsonProperty("startTime") String startTime, @JsonProperty("endTime") String endTime,
                             @JsonProperty("isDaytime") Boolean isDayTime,
                             @JsonProperty("temperature") Integer temperature,
                             @JsonProperty("temperatureUnit") String temperatureUnit,
                             @JsonProperty("temperatureTrend") String temperatureTrend,
                             @JsonProperty("probabilityOfPrecipitation") Map probabilityOfPrecipitation,
                             @JsonProperty("windSpeed") String windSpeed,
                             @JsonProperty("windDirection") String windDirection,
                             @JsonProperty("icon") String icon, @JsonProperty("shortForecast") String shortForecast,
                             @JsonProperty("detailedForecast") String detailedForecast) {
        }
    }


    @Tool(description = "Get the temperature (in celsius) for a specific location")
    public String getTemperature(@ToolParam(description = "The location latitude") double latitude,
                                 @ToolParam(description = "The location longitude") double longitude,
                                 ToolContext toolContext) {

        var points = restClient.get()
                .uri("/points/{latitude},{longitude}", latitude, longitude)
                .retrieve()
                .body(Points.class);
        log.info("start call Tool api.weather.gov");
        var forecast = restClient.get().uri(points.properties().forecast()).retrieve().body(Forecast.class);

        String forecastText = forecast.properties().periods().stream().map(p -> String.format("""
                        %s:
                        Temperature: %s %s
                        Wind: %s %s
                        Forecast: %s
                        """, p.name(), p.temperature(), p.temperatureUnit(), p.windSpeed(), p.windDirection(),
                p.detailedForecast())).collect(Collectors.joining());
        log.info("end call Tool api.weather.gov");


        return callMcpSampling(toolContext, forecastText);
    }

    public String callMcpSampling(ToolContext toolContext, String forecast) {

        StringBuilder openAiWeatherPoem = new StringBuilder();
        StringBuilder deepseekWeatherPoem = new StringBuilder();

        McpToolUtils.getMcpExchange(toolContext)
                .ifPresent(exchange -> {

                    exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.INFO)
                            .data("Start sampling")
                            .build());

                    if (exchange.getClientCapabilities().sampling() != null) {
                        var messageRequestBuilder = McpSchema.CreateMessageRequest.builder()
                                .systemPrompt("You are a poet!")
                                .messages(List.of(new McpSchema.SamplingMessage(McpSchema.Role.USER,
                                        new McpSchema.TextContent(
                                                "Please write a poem about this weather forecast (temperature is in Celsius). Use markdown format :\n "
                                                        + ModelOptionsUtils
                                                        .toJsonStringPrettyPrinter(forecast)))));


                        var opeAiLlmMessageRequest = messageRequestBuilder
                                .modelPreferences(McpSchema.ModelPreferences.builder().addHint("openai").build())
                                .build();
                        log.info("start call Mcp Tool sampling forecast OpenAI");
                        McpSchema.CreateMessageResult openAiLlmResponse = exchange.createMessage(opeAiLlmMessageRequest);
                        log.info("end call Mcp Tool sampling forecast OpenAI");
                        openAiWeatherPoem.append(((McpSchema.TextContent) openAiLlmResponse.content()).text());

                        var deepseekLlmMessageRequest = messageRequestBuilder
                                .modelPreferences(McpSchema.ModelPreferences.builder().addHint("deepseek").build())
                                .build();
                        log.info("start call Mcp Tool sampling forecast DeepSeek");
                        McpSchema.CreateMessageResult deepseekAiLlmResponse = exchange.createMessage(deepseekLlmMessageRequest);
                        log.info("end call Mcp Tool sampling forecast DeepSeek");
                        deepseekWeatherPoem.append(((McpSchema.TextContent) deepseekAiLlmResponse.content()).text());
                        //todo: 并发安全问题：issue：https://github.com/spring-projects/spring-ai/issues/4127
//                        CompletableFuture<Void> openai = CompletableFuture.runAsync(() -> {
//                            var opeAiLlmMessageRequest = messageRequestBuilder
//                                    .modelPreferences(McpSchema.ModelPreferences.builder().addHint("openai").build())
//                                    .build();
//                            log.info("start call Mcp Tool sampling forecast OpenAI");
//                            McpSchema.CreateMessageResult openAiLlmResponse = exchange.createMessage(opeAiLlmMessageRequest);
//                            log.info("end call Mcp Tool sampling forecast OpenAI");
//                            openAiWeatherPoem.append(((McpSchema.TextContent) openAiLlmResponse.content()).text());
//                        });
//                        CompletableFuture<Void> deepseek = CompletableFuture.runAsync(() -> {
//                            var deepseekLlmMessageRequest = messageRequestBuilder
//                                    .modelPreferences(McpSchema.ModelPreferences.builder().addHint("deepseek").build())
//                                    .build();
//                            log.info("start call Mcp Tool sampling forecast DeepSeek");
//                            McpSchema.CreateMessageResult deepseekAiLlmResponse = exchange.createMessage(deepseekLlmMessageRequest);
//                            log.info("end call Mcp Tool sampling forecast DeepSeek");
//                            deepseekWeatherPoem.append(((McpSchema.TextContent) deepseekAiLlmResponse.content()).text());
//                        });
//
//                        CompletableFuture.allOf(openai,deepseek).join();

                    }

                    exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.INFO)
                            .data("Finish Sampling")
                            .build());

                });


        return "OpenAI poem about the weather: " + openAiWeatherPoem.toString() + "\n\n" +
                "DeepSeek poem about the weather: " + deepseekWeatherPoem.toString() + "\n"
                + ModelOptionsUtils.toJsonStringPrettyPrinter(forecast);

    }


}