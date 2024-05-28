package dev.langchain4j.http.spring.restclient.logging;

import jdk.jfr.internal.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ResponseLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ResponseLoggingInterceptor.class);
    private final LogLevel logLevel;

    public ResponseLoggingInterceptor() {
        this(LogLevel.DEBUG);
    }

    public ResponseLoggingInterceptor(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        log(response);
        return response;
    }


    private void log(ClientHttpResponse response) {
        String message = "Response:\n - statuscode: {}\n - headers: {} - body: {}";

        try {
            switch (this.logLevel) {
                case INFO:
                    this.logInfo(response, message);
                    break;
                case WARN:
                    this.logWarn(response, message);
                    break;
                case ERROR:
                    this.logError(response, message);
                    break;
                default:
                    this.logDebug(response, message);
            }
        } catch (Exception exc) {
            log.warn("Failed to log response", exc);
        }

    }

    private void logInfo(ClientHttpResponse response, String message) {
        log.info(message, resolveCode(response), RequestLoggingInterceptor.inOneLine(response.getHeaders()), getBody(response));
    }

      private void logWarn(ClientHttpResponse response, String message) {
        log.warn(message, resolveCode(response), RequestLoggingInterceptor.inOneLine(response.getHeaders()), getBody(response))
        ;
    }

    private void logError(ClientHttpResponse response, String message) {
        log.error(message, resolveCode(response), RequestLoggingInterceptor.inOneLine(response.getHeaders()), getBody(response))
        ;
    }

    private void logDebug(ClientHttpResponse response, String message) {
        log.debug(message, resolveCode(response), RequestLoggingInterceptor.inOneLine(response.getHeaders()), getBody(response))
        ;
    }


    private Object resolveCode(ClientHttpResponse response) {
        try {
            int code = response.getStatusCode().value();
            String text = response.getStatusText();

            return code + ":" + text;
        } catch (IOException e) {
            log.warn("Exception happened while reading response status", e);
            return "[Exception happened while reading response status. Check logs for more details.]";
        }
    }

    private static String getBody(ClientHttpResponse response) {
        try {
            BufferedInputStream bis = new BufferedInputStream(response.getBody());
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            for (int result = bis.read(); result != -1; result = bis.read()) {
                buf.write((byte) result);
            }
            return buf.toString("UTF-8");
        } catch (IOException e) {
            log.warn("Exception happened while reading request body", e);
            return "[Exception happened while reading request body. Check logs for more details.]";
        }
    }


}
