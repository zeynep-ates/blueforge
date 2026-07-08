package com.blueforge.ai;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenRouterAiClient implements AiClient {

    private final RestClient restClient;
    private final String model;

    public OpenRouterAiClient(
            RestClient.Builder restClientBuilder,
            @Value("${blueforge.ai.openrouter.base-url}") String baseUrl,
            @Value("${blueforge.ai.openrouter.api-key}") String apiKey,
            @Value("${blueforge.ai.openrouter.model}") String model) {
        this.model = model;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public String complete(String prompt) {
        ChatCompletionResponse response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .body(new ChatCompletionRequest(model, List.of(new Message("user", prompt))))
                    .retrieve()
                    .body(ChatCompletionResponse.class);
        } catch (RestClientException e) {
            throw new AiClientException("OpenRouter request failed", e);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new AiClientException("OpenRouter returned no choices");
        }
        return response.choices().get(0).message().content();
    }

    private record ChatCompletionRequest(String model, List<Message> messages) {}

    private record Message(String role, String content) {}

    private record ChatCompletionResponse(List<Choice> choices) {}

    private record Choice(Message message) {}
}
