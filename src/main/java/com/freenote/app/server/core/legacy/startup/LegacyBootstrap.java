package com.freenote.app.server.core.legacy.startup;

import com.freenote.app.server.core.config.SSLConfig;
import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.PerConnectionHandler;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.core.legacy.socket.RawServerSocketProvider;
import com.freenote.app.server.core.legacy.socket.SSLServerSocketProvider;
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
    private ServerSocketProvider serverSocketProvider = new RawServerSocketProvider();
    private static final Logger log = LogManager.getLogger(LegacyBootstrap.class);

    public LegacyBootstrap() {
    }

    public static LegacyBootstrap createSSLBootstrap(SSLConfig sslConfig) {
        return new LegacyBootstrap(sslConfig);
    }

    private LegacyBootstrap(SSLConfig sslConfig) {
        this.serverSocketProvider = new SSLServerSocketProvider(sslConfig);
    }

    @Override
    public void start(PerConnectionHandler handler, ServerSocketConfig config) {
        logServerInitialization();
        try {
            logVirtualThreadWarn();
            try (var serverSocket = serverSocketProvider.createServerSocket(config)) {
                while (!serverSocket.isClosed()) {
                    log.info("Waiting for connection on port {}", config);
                    var clientSocket = serverSocket.accept(); // block method
                    var connectionContext = buildConnectionContext(clientSocket);
                    this.virtualExecutorService.submit(() -> perConnectionHandler(handler, connectionContext));
                }
            }
        } catch (Exception ex) {
            log.error("Error starting server", ex);
        }
    }

    private ConnectionContext buildConnectionContext(Socket clientSocket) {
        var networkRequestData = new BlockingNetworkRequestData(clientSocket);
        return ConnectionContext.builder()
                .networkRequestData(networkRequestData)
                .build();
    }

    private void perConnectionHandler(PerConnectionHandler perConnectionHandler, ConnectionContext connectionContext) {
        try {
            perConnectionHandler.handle(connectionContext);
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
