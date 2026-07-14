package com.simpatbos.startstopplugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RequestManager<T> {
    private final Class<T> responseType;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequestManager(Class<T> responseType) {
        this.responseType = responseType;
    }

    public T requestUrl(String uri) throws InterruptedException, IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        T parsed = objectMapper.readValue(response.body(), this.responseType);

        return parsed;
    }
}
