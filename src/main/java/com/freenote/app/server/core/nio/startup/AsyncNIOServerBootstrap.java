package com.freenote.app.server.core.nio.startup;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.nio.ConnectionPipeline;
import com.freenote.app.server.core.nio.sessions.AsyncNIOServerSession;
import com.freenote.app.server.core.startup.ServerBootstrap;
import com.freenote.app.server.exceptions.NIOServerInitializationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.AbstractExecutorService;

import static com.freenote.app.server.util.RuntimeUtils.logServerInitialization;

public class AsyncNIOServerBootstrap implements ServerBootstrap {
    private static final Logger log = LogManager.getLogger(AsyncNIOServerBootstrap.class);
    private AbstractExecutorService virtualExecutorService;


    @Override
    public void start(IncomingConnectionHandler connectionHandler, ServerSocketConfig socketConfig) throws Exception {
        try (var serverSocketChannel = tryOpenSocketChannel(socketConfig)) {
            var connectionPipeline = new ConnectionPipeline(connectionHandler);
            var nioServerSession = buildServerSession(serverSocketChannel, connectionPipeline);
            logServerInitialization();
            nioServerSession.registerReadEvent();
            Thread.currentThread().join();
        }
    }

    private AsyncNIOServerSession buildServerSession(AsynchronousServerSocketChannel serverSocketChannel, ConnectionPipeline connectionPipeline) {
        return AsyncNIOServerSession.builder()
                .asyncServerChannel(serverSocketChannel)
                .pipeline(connectionPipeline)
                .build();
    }

    private AsynchronousServerSocketChannel tryOpenSocketChannel(ServerSocketConfig socketConfig) {
        try {
            var asyncServerSocketChannel = AsynchronousServerSocketChannel.open();
            asyncServerSocketChannel.bind(new InetSocketAddress(socketConfig.port()));
            return asyncServerSocketChannel;
        } catch (IOException e) {
            throw new NIOServerInitializationException("Failed to open server socket channel", e);
        }
    }
}
