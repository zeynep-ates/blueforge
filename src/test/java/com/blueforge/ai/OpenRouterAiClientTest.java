package com.blueforge.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenRouterAiClientTest {

    @Test
    void completeReturnsMessageContentFromFirstChoice() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess(
                        """
                        {
                          "choices": [
                            { "message": { "role": "assistant", "content": "Hello there" } }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        OpenRouterAiClient client =
                new OpenRouterAiClient(builder, "https://openrouter.ai/api/v1", "test-key", "test-model");

        String result = client.complete("Say hi");

        assertThat(result).isEqualTo("Hello there");
        server.verify();
    }

    @Test
    void completeThrowsAiClientExceptionWhenNoChoicesReturned() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andRespond(withSuccess("""
                        { "choices": [] }
                        """, MediaType.APPLICATION_JSON));

        OpenRouterAiClient client =
                new OpenRouterAiClient(builder, "https://openrouter.ai/api/v1", "test-key", "test-model");

        org.junit.jupiter.api.Assertions.assertThrows(
                AiClientException.class, () -> client.complete("Say hi"));
    }
}
