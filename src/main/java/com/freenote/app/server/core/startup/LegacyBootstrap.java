package com.freenote.app.server.core.startup;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.connection.WebSocketSession;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.io.socket.RawSocket;
import com.freenote.app.server.io.socket.ServerSocketFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.freenote.app.server.util.RuntimeUtils.getAvailableProcessors;
import static com.freenote.app.server.util.RuntimeUtils.logServerInitialization;

public class LegacyBootstrap implements ServerBootstrap {
    private ExecutorService executorService = Executors.newFixedThreadPool(getAvailableProcessors());
    private ExecutorService virtualExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    private ServerSocketFactory serverSocketFactory = new RawSocket();
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
        try (var serverSocket = serverSocketFactory.createServerSocket(config)) {
            while (!serverSocket.isClosed()) {
                log.info("Waiting for connection on port {}", config);
                var socket = serverSocket.accept(); // block method
                log.info("Accepted connection from {}", socket.getRemoteSocketAddress());
                this.virtualExecutorService.submit(() -> {
                    try {
                        var session = WebSocketSession.builder()
                                .socket(socket)
                                .inputWrapper(new InputWrapper(socket))
                                .outputWrapper(new OutputWrapper(socket.getOutputStream()))
                                .build();
                        var connectionContext = ConnectionContext.builder()
                                .socket(socket)
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
