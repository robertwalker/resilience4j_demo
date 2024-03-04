package com.robertwalker.resiliencedemo.resiliencedemo.service;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.robertwalker.resiliencedemo.resiliencedemo.service.ServiceApi;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates Resilience4J's TimeLimiter, Retry and CircuitBreaker features.
 * <p>
 * Note: For demonstration purposes this test suite MUST be executed in the designated order, otherwise the metrics
 *       would need to be reset between each test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WireMockTest(httpPort = 8888)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BackendAServiceTest {
    @LocalServerPort
    private int port;

    @Autowired
    private ServiceApi serviceApi;

    @Test
    @DisplayName("should call the external service successfully given a 1 second delay")
    @Order(1)
    void externalServiceCall() {
        // Setup WireMock
        stubFor(get(urlPathEqualTo("/slow/service"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Hello, World!")
                        .withFixedDelay(1_000)));

        // When
        long startTime = System.currentTimeMillis();
        Mono<String> response = serviceApi.externalServiceCall();
        String body = response.block();
        long duration = System.currentTimeMillis() - startTime;

        // Then
        verify(1, getRequestedFor(urlPathEqualTo("/slow/service")));
        assertThat(duration).isLessThan(2_500L);
        assertThat(body).isEqualTo("Hello, World!");

        // And: the time limiter records 1 successful call
        String timeLimiterSuccessful = getMetrics("resilience4j.timelimiter.calls?tag=kind:successful");
        assertThat(timeLimiterSuccessful).contains("{\"statistic\":\"COUNT\",\"value\":1.0}");

        // And: retry records 1 successful call without retry
        String metrics = getMetrics("resilience4j.retry.calls?tag=kind:successful_without_retry");
        assertThat(metrics).contains("{\"statistic\":\"COUNT\",\"value\":1.0}");
    }

    @Test
    @DisplayName("should handled fault response with the retry fallback handler")
    @Order(2)
    void externalServiceCall_FailureWithRetry() {
        // Setup WireMock
        stubFor(get(urlPathEqualTo("/slow/service"))
                .willReturn(aResponse()
                        .withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

        // When
        Mono<String> response = serviceApi.externalServiceCall();
        String body = response.block();

        // Then
        verify(2, getRequestedFor(urlPathEqualTo("/slow/service")));
        assertThat(body).isEqualTo("Exception handled by retry fallback");

        // And: retry records 1 failed call with retry
        String metrics = getMetrics("resilience4j.retry.calls?tag=kind:failed_with_retry");
        assertThat(metrics).contains("{\"statistic\":\"COUNT\",\"value\":1.0}");
    }

    @Test
    @DisplayName("should handled timeout with the timeout fallback handler given a 3 second delay")
    @Order(3)
    void externalServiceCall_Timeout() {
        // Setup WireMock
        stubFor(get(urlPathEqualTo("/slow/service"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Hello, World!")
                        .withFixedDelay(3_000)));

        // When
        Mono<String> response = serviceApi.externalServiceCall();
        String body = response.block();

        // Then
        verify(1, getRequestedFor(urlPathEqualTo("/slow/service")));
        assertThat(body).isEqualTo("Exception handled by time limiter fallback");

        // And: the time limiter records 1 timeout call
        String metrics = getMetrics("resilience4j.timelimiter.calls?tag=kind:timeout");
        assertThat(metrics).contains("{\"statistic\":\"COUNT\",\"value\":1.0}");
    }

    @Test
    @DisplayName("should handled opening the circuit breaker and call the circuit breaker fallback handler")
    @Order(4)
    void externalServiceCall_CircuitBreakerOpens() {
        // Setup WireMock
        stubFor(get(urlPathEqualTo("/slow/service"))
                .willReturn(aResponse()
                        .withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

        // Given
        int callCount = 6;

        // When
        for (int i = 0; i < callCount; i++) {
            Mono<String> response = serviceApi.externalServiceCall();
            response.block();
        }

        // Then: 2 of the 6 calls are block by the open circuit breaker
        verify(4, getRequestedFor(urlPathEqualTo("/slow/service")));

        // And: the first 3 calls are counted as retries
        String metrics = getMetrics("resilience4j.retry.calls?tag=kind:failed_with_retry");
        assertThat(metrics).contains("{\"statistic\":\"COUNT\",\"value\":3.0}");

        // And: the circuit breaker records 5 total calls
        String calls = getMetrics("resilience4j.circuitbreaker.calls");
        assertThat(calls).contains("{\"statistic\":\"COUNT\",\"value\":5.0}");

        // And: the circuit breaker records 1 successful call
        String successful = getMetrics("resilience4j.circuitbreaker.calls?tag=kind:successful");
        assertThat(successful).contains("{\"statistic\":\"COUNT\",\"value\":1.0}");

        // And: the circuit breaker records 4 failed calls
        String failed = getMetrics("resilience4j.circuitbreaker.calls?tag=kind:failed");
        assertThat(failed).contains("{\"statistic\":\"COUNT\",\"value\":4.0}");

        // And: the circuit breaker records 4 blocked (a.k.a. not permitted) calls
        String ignored = getMetrics("/resilience4j.circuitbreaker.not.permitted.calls?tag=kind:not_permitted");
        assertThat(ignored).contains("{\"statistic\":\"COUNT\",\"value\":4.0}");
    }

    //---- Private Methods ----//

    private String getMetrics(String metricName) {
        String url = "http://localhost:" + port + "/actuator/metrics/" + metricName;
        WebClient client = WebClient.builder().build();
        Mono<String> response = client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);
        return response.block();
    }
}
