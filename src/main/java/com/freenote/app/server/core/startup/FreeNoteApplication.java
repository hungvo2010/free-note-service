package com.freenote.app.server.core.startup;

import com.freenote.app.server.config.SSLConfig;
import com.freenote.app.server.config.ServerSocketConfig;
import com.freenote.app.server.config.datasources.ConfigRepository;
import com.freenote.app.server.core.legacy.WebSocketServer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.SampleGlobalOpenTelemetry;

import java.util.Optional;

import static otel.sdk.provider.OpenTelemetrySdkConfig.create;

public class FreeNoteApplication {
    private static final Logger log = LogManager.getLogger(FreeNoteApplication.class);

    public void run(Integer portOverride, String serverTypeOverride) {
        try {
            var configRepo = new ConfigRepository();
            configRepo.load();
            var activeProfile = configRepo.getActiveProfile();
            if (activeProfile != null && !activeProfile.equals("default")) {
                configRepo.loadSource(configRepo.resolve(activeProfile));
            }

            GlobalOpenTelemetry.set(create());
            SampleGlobalOpenTelemetry.init();
            var startingPort = portOverride != null ? portOverride
                    : Integer.parseInt(Optional.ofNullable(configRepo.get("freenote.active.port")).orElse("8888"));

            var sslEnabled = Boolean.parseBoolean(configRepo.getOrDefault("server.ssl.enabled", "false"));
            WebSocketServer server = WebSocketServer.builder()
                    .sslConfig(
                            sslEnabled ?
                                    new SSLConfig(
                                            configRepo.getOrDefault("server.ssl.keystore.path", "keystore.p12"),
                                            configRepo.getOrDefault("server.ssl.keystore.password", "changeit"))
                                    : null
                    )
                    .socketConfig(new ServerSocketConfig(startingPort))
                    .serverType(
                            serverTypeOverride != null ? serverTypeOverride
                                    : Optional.ofNullable(configRepo.get("freenote.server.type"))
                                            .orElse("thread-per-connection")
                    )
                    .build();
            server.start();
        } catch (Exception ex) {
            log.error("Error starting application", ex);
        }
    }

    public static void main(String[] args) {
        new FreeNoteApplication().run(
                args.length >= 1 ? Integer.parseInt(args[0]) : null,
                args.length >= 2 ? args[1] : null
        );
    }
}
