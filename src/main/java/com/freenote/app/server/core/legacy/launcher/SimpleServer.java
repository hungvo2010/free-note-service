package com.freenote.app.server.core.legacy.launcher;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.legacy.DefaultLegacySessionBasedConnectionHandler;
import com.freenote.app.server.core.legacy.LegacyConnectionAdapter;
import com.freenote.app.server.core.legacy.WebSocketServer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import otel.SampleGlobalOpenTelemetry;

import static otel.sdk.provider.OpenTelemetrySdkConfig.create;

public class SimpleServer {

    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        run(port);
    }

    public static void run(int port) throws Exception {
        GlobalOpenTelemetry.set(create());
        SampleGlobalOpenTelemetry.init();
        WebSocketServer server = WebSocketServer.builder()
                .socketConfig(new ServerSocketConfig(port))
                .handler(new LegacyConnectionAdapter(new DefaultLegacySessionBasedConnectionHandler()))
                .build();
        server.start();
    }
}
