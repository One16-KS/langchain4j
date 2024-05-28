package dev.langchain4j.http.spring.restclient;

import dev.langchain4j.http.HttpClient;
import dev.langchain4j.http.HttpClientBuilder;
import dev.langchain4j.http.HttpMethod;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringRestClientHttpClientTest {

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startServer() {
        mockServer = ClientAndServer.startClientAndServer(1080);
    }

    @AfterAll
    public static void stopServer() {
        mockServer.stop();
    }


    void setupServer(int statusCode, String statusText, String body) {
        mockServer.when(HttpRequest.request().withPath("/hello")).respond(HttpResponse.response()
                .withStatusCode(statusCode)
                .withReasonPhrase(statusText)
                .withHeaders(
                        new Header("Content-Type", "application/json; charset=utf-8"),
                        new Header("Cache-Control", "public, max-age=86400")
                )
                .withBody(body)
                .withDelay(TimeUnit.SECONDS, 1)
        );
    }


    @Test
    void test200(){
        String body ="{ 'message': 'bliblablu' }";
        setupServer(200, "All Good", body);
        dev.langchain4j.http.HttpResponse response = hitTheServerWithPostRequest(body);
        assertEquals(response.body(), body);
    }


    private dev.langchain4j.http.HttpResponse hitTheServerWithPostRequest(String body) {
        String url = "http://127.0.0.1:1080/hello";
        HttpClientBuilder builder = new SpringRestClientHttpClientBuilderFactory().create();

        HttpClient httpClient = builder.logRequests(true).logResponses(true).build();

        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-type", "application/json");

        dev.langchain4j.http.HttpRequest post = new dev.langchain4j.http.HttpRequest(HttpMethod.POST, url, headerMap, body);
        dev.langchain4j.http.HttpResponse response = null;

        try {
            response = httpClient.execute(post);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}




