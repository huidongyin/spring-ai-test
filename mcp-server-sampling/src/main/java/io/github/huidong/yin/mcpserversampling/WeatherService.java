package io.github.huidong.yin.mcpserversampling;

import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WeatherService {

	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(WeatherService.class);

	private final RestClient restClient;

	public WeatherService() {
		this.restClient = RestClient.create();
	}

	/**
	 * The response format from the Open-Meteo API
	 */
	public record WeatherResponse(Current current) {
		public record Current(LocalDateTime time, int interval, double temperature_2m) {
		}
	}

	@Tool(description = "Get the temperature (in celsius) for a specific location")
	public String getTemperature(@ToolParam(description = "The location latitude") double latitude,
			@ToolParam(description = "The location longitude") double longitude,
			ToolContext toolContext) {

		WeatherResponse weatherResponse = new WeatherResponse(new WeatherResponse.Current(LocalDateTime.now(),10,100));
//				restClient
//				.get()
//				.uri("http://api.open-meteo.com/v1/forecast?latitude={latitude}&longitude={longitude}&current=temperature_2m",
//						latitude, longitude)
//				.retrieve()
//				.body(WeatherResponse.class);

		String responseWithPoems = callMcpSampling(toolContext, weatherResponse);

		return responseWithPoems;
	}

	public String callMcpSampling(ToolContext toolContext, WeatherResponse weatherResponse) {

		StringBuilder openAiWeatherPoem = new StringBuilder();
		StringBuilder anthropicWeatherPoem = new StringBuilder();

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
												"Please write a poem about thius weather forecast (temperature is in Celsious). Use markdown format :\n "
														+ ModelOptionsUtils
																.toJsonStringPrettyPrinter(weatherResponse)))));

						var opeAiLlmMessageRequest = messageRequestBuilder
								.modelPreferences(McpSchema.ModelPreferences.builder().addHint("openai").build())
								.build();
						McpSchema.CreateMessageResult openAiLlmResponse = exchange.createMessage(opeAiLlmMessageRequest);

						openAiWeatherPoem.append(((McpSchema.TextContent) openAiLlmResponse.content()).text());

						var anthropicLlmMessageRequest = messageRequestBuilder
								.modelPreferences(McpSchema.ModelPreferences.builder().addHint("anthropic").build())
								.build();
						McpSchema.CreateMessageResult anthropicAiLlmResponse = exchange.createMessage(anthropicLlmMessageRequest);

						anthropicWeatherPoem.append(((McpSchema.TextContent) anthropicAiLlmResponse.content()).text());

					}

					exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
							.level(McpSchema.LoggingLevel.INFO)
							.data("Finish Sampling")
							.build());

				});

		String responseWithPoems = "OpenAI poem about the weather: " + openAiWeatherPoem.toString() + "\n\n" +
				"Anthropic poem about the weather: " + anthropicWeatherPoem.toString() + "\n"
				+ ModelOptionsUtils.toJsonStringPrettyPrinter(weatherResponse);

		logger.info(anthropicWeatherPoem.toString(), responseWithPoems.toString());

		return responseWithPoems;

	}

}