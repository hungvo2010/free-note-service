package com.freenote.app.server.core.legacy.startup;

import com.freenote.app.server.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.PerClientConnectionHandler;
import com.freenote.app.server.core.event.ConnectionEvent;
import com.freenote.app.server.core.legacy.socket.ServerSocketProvider;
import com.freenote.app.server.core.startup.ServerBootstrap;
import com.freenote.app.server.model.ws.BlockingNetworkRequestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.freenote.app.server.util.RuntimeUtils.logServerInitialization;

public class LegacyBootstrap implements ServerBootstrap {
    private ExecutorService virtualExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    private ServerSocketProvider serverSocketProvider = new ServerSocketProvider.RawServerSocketProvider();
    private static final Logger log = LogManager.getLogger(LegacyBootstrap.class);

    public LegacyBootstrap() {
    }

    public static LegacyBootstrap createSSLBootstrap(ServerSocketConfig.SSLConfig sslConfig) {
        return new LegacyBootstrap(sslConfig);
    }

    private LegacyBootstrap(ServerSocketConfig.SSLConfig sslConfig) {
        this.serverSocketProvider = new ServerSocketProvider.SSLServerSocketProvider(sslConfig);
    }

    @Override
    public void start(PerClientConnectionHandler handler, ServerSocketConfig config) {
        logServerInitialization();
        try {
            logVirtualThreadWarn();
            try (var serverSocket = serverSocketProvider.createServerSocket(config)) {
                while (!serverSocket.isClosed()) {
                    log.info("Waiting for connection on port {}", config);
                    var clientSocket = serverSocket.accept(); // block method
                    var connectionEvent = buildConnectionEvent(clientSocket);
                    this.virtualExecutorService.submit(() -> perConnectionHandler(handler, connectionEvent));
                }
            }
        } catch (Exception ex) {
            log.error("Error starting server", ex);
        }
    }

    private ConnectionEvent buildConnectionEvent(Socket clientSocket) {
        var networkRequestData = new BlockingNetworkRequestData(clientSocket);
        return ConnectionEvent.builder()
                .networkRequestData(networkRequestData)
                .build();
    }

    private void perConnectionHandler(PerClientConnectionHandler perConnectionHandler, ConnectionEvent connectionEvent) {
        try {
            perConnectionHandler.handle(connectionEvent);
        } catch (Exception e) {
            log.error("Error handling connection", e);
        }
    }

    private void logVirtualThreadWarn() throws InterruptedException {
        Thread t = Thread.ofVirtual()
                .name("my-worker")
                .unstarted(() -> {
                    log.warn("Running in virtual thread: {}, Is Virtual: {}", Thread.currentThread(), Thread.currentThread().isVirtual());
                });
        t.start();
        t.join();
    }
}
