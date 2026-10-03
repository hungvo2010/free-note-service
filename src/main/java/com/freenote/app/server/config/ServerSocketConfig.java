package com.freenote.app.server.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ServerSocketConfig {
    private final int port;

    public ServerSocketConfig(int port) {
        this.port = port;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    public static class SSLConfig {
        @Builder.Default
        private String keystorePath = "keystore.p12";
        @Builder.Default
        private String keystorePassword = "changeit";
    }
}
