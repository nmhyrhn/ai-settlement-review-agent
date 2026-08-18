package com.namhyerin.settlement.review.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.namhyerin.settlement.review.application.AiReviewExplainer;
import com.namhyerin.settlement.review.application.AiReviewExplanation;
import com.namhyerin.settlement.review.domain.ReviewViolation;
import com.namhyerin.settlement.review.domain.SettlementTransaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Primary
@Component
public class OpenAiReviewExplainer implements AiReviewExplainer {

    private static final URI ENDPOINT = URI.create("https://api.openai.com/v1/chat/completions");

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final FallbackReviewExplainer fallback;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public OpenAiReviewExplainer(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4.1-mini}") String model,
            ObjectMapper objectMapper,
            FallbackReviewExplainer fallback
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
    }

    @Override
    public AiReviewExplanation explain(SettlementTransaction transaction, List<ReviewViolation> violations) {
        if (apiKey.isBlank() || violations.isEmpty()) return fallback.explain(transaction, violations);

        try {
            String input = objectMapper.writeValueAsString(Map.of("transaction", transaction, "violations", violations));
            Map<String, Object> schema = Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "summary", Map.of("type", "string"),
                            "checkPoints", Map.of("type", "array", "items", Map.of("type", "string")),
                            "suggestedAction", Map.of("type", "string", "enum", List.of("APPROVE", "RECHECK", "HOLD")),
                            "available", Map.of("type", "boolean", "const", true)),
                    "required", List.of("summary", "checkPoints", "suggestedAction", "available"),
                    "additionalProperties", false);
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "정산 규칙 판정은 바꾸지 말고, 담당자가 확인할 내용을 간결한 한국어로 설명하세요."),
                            Map.of("role", "user", "content", input)),
                    "response_format", Map.of("type", "json_schema", "json_schema",
                            Map.of("name", "settlement_review", "strict", true, "schema", schema)));
            HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) return fallback.explain(transaction, violations);
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            return objectMapper.readValue(content, AiReviewExplanation.class);
        } catch (Exception e) {
            return fallback.explain(transaction, violations);
        }
    }
}
