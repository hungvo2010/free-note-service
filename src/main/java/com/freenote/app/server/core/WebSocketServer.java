package com.freenote.app.server.core;

import com.freenote.app.server.config.ServerSocketConfig;
import com.freenote.app.server.endpoints.EndpointResolver;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Builder
public class WebSocketServer {
    private static final Logger log = LogManager.getLogger(WebSocketServer.class);
    private ServerSocketConfig socketConfig;
    private ServerSocketConfig.SSLConfig sslConfig;
    private String serverType;
    private EndpointResolver endpointResolver;

    public void start() throws Exception {
        log.info("Starting WebSocket Server on port {}", socketConfig.getPort());
        var ioModel = IoModel.of(serverType, sslConfig, endpointResolver);
        ioModel.acceptLoop().start(ioModel.handler(), socketConfig);
    }
}
