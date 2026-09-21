package com.freenote.app.server.core.legacy;

import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.startup.IoModel;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Builder
public class WebSocketServer {
    private static final Logger log = LogManager.getLogger(WebSocketServer.class);
    private ServerSocketConfig socketConfig;
    private SSLConfig sslConfig;
    private String serverType;

    public void start() throws Exception {
        log.info("Starting WebSocket Server on port {}", socketConfig.port());
        var ioModel = IoModel.of(serverType, sslConfig);
        ioModel.acceptLoop().start(ioModel.handler(), socketConfig);
    }
}
