package com.robertwalker.resiliencedemo.resiliencedemo.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class BackendAService implements ServiceApi {
    private static final String BACKEND = "backendA";

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10_000;

    private final WebClient client;

    /**
     * This constructor creates the WebClient with default timeouts. These timeouts serve as a backup to the
     * Resilience4J TimeLimiter (2 seconds) to ensure that all external connections are completed within 5 seconds
     * and responses are received in under 10 seconds.
     */
    public BackendAService() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
                .responseTimeout(Duration.ofMillis(READ_TIMEOUT))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(READ_TIMEOUT, TimeUnit.MILLISECONDS)));
        client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * Calls an emulated service provided by WireMock for testing Resilience4J TimeLimiter.
     *
     * @return the Mono response future
     */
    @Override
    @CircuitBreaker(name = BACKEND, fallbackMethod = "fallback")
    @TimeLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    public Mono<String> externalServiceCall() {
        String url = "http://localhost:8888/slow/service";
        log.info("Calling: {}", url);
        return client.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Handles calls blocked by an "Open" CircuitBreaker.
     *
     * @param e the circuit breaker call not permitted exception
     * @return the Mono response future
     */
    @SuppressWarnings("unused")
    private Mono<String> fallback(CallNotPermittedException e) {
        log.error("CircuitBreaker fallback was called", e);
        return Mono.just("Exception handled by circuit breaker fallback");
    }

    /**
     * Handles timeouts triggered by the TimeLimiter.
     *
     * @param e the timeout exception
     * @return the Mono response future
     */
    @SuppressWarnings("unused")
    private Mono<String> fallback(TimeoutException e) {
        log.error("Timeout fallback was called", e);
        return Mono.just("Exception handled by time limiter fallback");
    }

    /**
     * Handles external call failures after hitting max retries.
     *
     * @param e the WebClient exception
     * @return the Mono response future
     */
    @SuppressWarnings("unused")
    private Mono<String> fallback(WebClientResponseException e) {
        log.error("Retry fallback was called", e);
        return Mono.just("Exception handled by retry fallback");
    }

    /**
     * Fallback handler for other unhandled exceptions.
     *
     * @param e the unhandled exception
     * @return the Mono response future
     */
    @SuppressWarnings("unused")
    private Mono<String> fallback(Exception e) {
        log.error("Unhandled exception fallback was called", e);
        return Mono.just("Exception handled by catch all fallback");
    }
}
