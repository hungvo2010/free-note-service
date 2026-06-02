package com.freenote.app.server.core.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class SSLConfig {
    @Builder.Default
    private String keystorePath = "keystore.p12";
    @Builder.Default
    private String keystorePassword = "changeit";
}
