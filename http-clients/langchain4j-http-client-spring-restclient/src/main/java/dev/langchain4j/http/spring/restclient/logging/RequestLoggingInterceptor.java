package dev.langchain4j.http.spring.restclient.logging;

import jdk.jfr.internal.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNullApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RequestLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final Set<String> COMMON_SECRET_HEADERS = new HashSet(Arrays.asList("authorization", "x-api-key", "x-auth-token"));
    private final LogLevel logLevel;

    public RequestLoggingInterceptor() {
        this(LogLevel.DEBUG);
    }

    public RequestLoggingInterceptor(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        this.log(request, body);
        return execution.execute(request, body);
    }


    private void log(HttpRequest request, byte[] body) {
        String message = "Request:\n- method: {}\n- url: {}\n- headers: {}\n- body: {}";

        try {
            switch (this.logLevel) {
                case INFO:
                    this.logInfo(request, message, body);
                    break;
                case WARN:
                    this.logWarn(request, message, body);
                    break;
                case ERROR:
                    this.logError(request, message, body);
                    break;
                default:
                    this.logDebug(request, message, body);
            }
        } catch (Exception exc) {
            log.warn("Failed to log request", exc);
        }

    }

    private void logInfo(HttpRequest request, String message, byte[] body) {
        log.info(message, request.getMethod(), request.getURI(), inOneLine(request.getHeaders()), getBody(body));
    }

    private void logWarn(HttpRequest request, String message, byte[] body) {
        log.warn(message, request.getMethod(), request.getURI(), inOneLine(request.getHeaders()), getBody(body));
    }

    private void logError(HttpRequest request, String message, byte[] body) {
        log.error(message, request.getMethod(), request.getURI(), inOneLine(request.getHeaders()), getBody(body));
    }

    private void logDebug(HttpRequest request, String message, byte[] body) {
        log.debug(message, request.getMethod(), request.getURI(), inOneLine(request.getHeaders()), getBody(body));
    }

    static String inOneLine(HttpHeaders headers) {
        return headers.toSingleValueMap().entrySet().stream().map((header) -> format(header.getKey(), header.getValue())).collect(Collectors.joining(", "));
    }

    static String format(String headerKey, String headerValue) {
        if (COMMON_SECRET_HEADERS.contains(headerKey.toLowerCase())) {
            headerValue = maskSecretKey(headerValue);
        }

        return String.format("[%s: %s]", headerKey, headerValue);
    }

    static String maskSecretKey(String key) {
        if (key != null && !key.trim().isEmpty()) {
            try {
                return key.startsWith("Bearer") ? "Bearer " + mask(key.substring("Bearer".length() + 1)) : mask(key);
            } catch (Exception var2) {
                return "Failed to mask the API key.";
            }
        } else {
            return key;
        }
    }

    private static String mask(String key) {
        return key.length() >= 7 ? key.substring(0, 5) + "..." + key.substring(key.length() - 2) : "...";
    }

    private static String getBody(byte[] body) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            stream.write(body);
            return stream.toString("UTF-8");
        } catch (IOException e) {
                log.warn("Exception happened while reading request body", e);
                return "[Exception happened while reading request body. Check logs for more details.]";
        }
    }


}
