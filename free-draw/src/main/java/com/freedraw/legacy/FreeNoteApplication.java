package com.freedraw.legacy;

import com.freedraw.common.EnvironmentVariable;
import com.freedraw.dto.DraftResponseData;
import com.freedraw.resources.RedisClient;
import com.freedraw.resources.RedisKeys;
import com.freenote.app.server.config.SSLConfig;
import com.freenote.app.server.config.ServerSocketConfig;
import com.freenote.app.server.config.datasources.ConfigRepository;
import com.freenote.app.server.core.legacy.WebSocketServer;
import com.freenote.app.server.endpoints.URIEndpointHandler;
import com.freenote.app.server.util.JSONUtils;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.SampleGlobalOpenTelemetry;

import java.util.Optional;

import static otel.sdk.provider.OpenTelemetrySdkConfig.create;

public class FreeNoteApplication {
    private static final Logger log = LogManager.getLogger(FreeNoteApplication.class);

    public void run() {
        try {
            var configRepo = new ConfigRepository();
            configRepo.load();
            var activeProfile = configRepo.getActiveProfile();
            if (activeProfile != null && !activeProfile.equals("default")) {
                configRepo.loadSource(configRepo.resolve(activeProfile));
            }

            GlobalOpenTelemetry.set(create());
            SampleGlobalOpenTelemetry.init();
            var startingPort = Integer.parseInt(
                    Optional.ofNullable(configRepo.get("freenote.active.port")).orElse("8888")
            );

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
                            Optional.ofNullable(configRepo.get("freenote.server.type"))
                                    .orElse("thread-per-connection")
                    )
                    .endpointResolver(path -> (URIEndpointHandler) generated.URIHandlerRegistry.getInstanceByURI(path))
                    .build();
            initPubSubs();
            server.start();
        } catch (Exception ex) {
            log.error("Error starting application", ex);
        }
    }

    private void initPubSubs() {
        var redisClient = RedisClient.getRedissonClient();
        var serverName = EnvironmentVariable.getHostName();
        var subscribeTopic = redisClient.getTopic(RedisKeys.SERVER_PREFIX + serverName);
        subscribeTopic.addListenerAsync(String.class, (channel, message) -> {
            try {
                log.info("[PUBSUB] Received message: {}", message);
                var messageData = JSONUtils.fromJSON(message, DraftResponseData.class);
                if (messageData == null) {
                    log.warn("[PUBSUB] Dropping unparseable message");
                    return;
                }
                ConnectionsRegistry.sendToMember(messageData);
            } catch (Exception e) {
                log.error("[PUBSUB] Failed to handle message", e);
            }
        });
    }

    public static void main(String[] args) {
        var freeNoteApp = new FreeNoteApplication();
        freeNoteApp.run();
    }
}
