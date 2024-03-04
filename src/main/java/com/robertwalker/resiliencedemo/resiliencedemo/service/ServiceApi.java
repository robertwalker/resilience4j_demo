package com.robertwalker.resiliencedemo.resiliencedemo.service;

import reactor.core.publisher.Mono;

public interface ServiceApi {
    Mono<String> externalServiceCall();
}
