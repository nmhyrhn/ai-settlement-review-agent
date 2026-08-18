package com.namhyerin.settlement.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Base64;

@Component
public class PolicyRagClient {

    private final RestClient restClient;
    private final String apiKey;

    public PolicyRagClient(RestClient.Builder builder,
                           @Value("${rag.service.url}") String serviceUrl,
                           @Value("${rag.service.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 로컬 내부 서비스 호출이 운영체제 프록시를 경유하지 않도록 고정함
        requestFactory.setProxy(java.net.Proxy.NO_PROXY);
        this.restClient = builder.baseUrl(serviceUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey;
    }

    public void index(long id, String title, int version, String filename, String contentType, byte[] content) {
        restClient.post()
                .uri("/internal/documents")
                .header("X-Internal-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new IndexRequest(id, title, version, filename, contentType,
                        Base64.getEncoder().encodeToString(content)))
                .retrieve()
                .toBodilessEntity();
    }

    record IndexRequest(long documentId, String title, int version, String filename,
                        String contentType, String contentBase64) {
    }
}
