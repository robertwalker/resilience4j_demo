package com.robertwalker.resiliencedemo.resiliencedemo.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BackendMessage {
    String message;
}
