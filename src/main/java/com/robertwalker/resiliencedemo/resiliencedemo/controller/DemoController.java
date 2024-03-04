package com.robertwalker.resiliencedemo.resiliencedemo.controller;

import com.robertwalker.resiliencedemo.resiliencedemo.model.BackendMessage;
import com.robertwalker.resiliencedemo.resiliencedemo.service.BackendAService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class DemoController {
    private final BackendAService backendAService;

    public DemoController(BackendAService backendAService) {
        this.backendAService = backendAService;
    }

    @GetMapping("/")
    public BackendMessage rootPath() {
        Mono<String> response = backendAService.externalServiceCall();
        return BackendMessage.builder()
                .message(response.block())
                .build();
    }
}
