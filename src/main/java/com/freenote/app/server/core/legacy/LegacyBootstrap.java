package com.freenote.app.server.core.legacy;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.core.startup.ServerBootstrap;
import com.freenote.app.server.io.socket.RawServerSocketProvider;
import com.freenote.app.server.io.socket.ServerSocketProvider;
import com.freenote.app.server.model.ws.BlockingNetworkRequestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.freenote.app.server.util.RuntimeUtils.logServerInitialization;

public class LegacyBootstrap implements ServerBootstrap {
    private ExecutorService virtualExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    private ServerSocketProvider serverSocketProvider = new RawServerSocketProvider();
    private static final Logger log = LogManager.getLogger(LegacyBootstrap.class);

    public void start(IncomingConnectionHandler handler, ServerSocketConfig config) throws Exception {
        logServerInitialization();
        Thread t = Thread.ofVirtual()
                .name("my-worker")
                .unstarted(() -> {
                    log.warn("Running in virtual thread: {}, Is Virtual: {}", Thread.currentThread(), Thread.currentThread().isVirtual());
                });

        t.start();
        t.join();
        try (var serverSocket = serverSocketProvider.createServerSocket(config)) {
            while (!serverSocket.isClosed()) {
                log.info("Waiting for connection on port {}", config);
                var socket = serverSocket.accept(); // block method
                log.info("Accepted connection from {}", socket.getRemoteSocketAddress());
                this.virtualExecutorService.submit(() -> {
                    try {
                        var networkRequestData = new BlockingNetworkRequestData(socket);
                        var connectionContext = ConnectionContext.builder()
                                .networkRequestData(networkRequestData)
                                .build();
                        handler.handle(connectionContext);
                    } catch (Exception e) {
                        log.error("Error handling connection", e);
                    }
                });
            }
        }
    }
}
