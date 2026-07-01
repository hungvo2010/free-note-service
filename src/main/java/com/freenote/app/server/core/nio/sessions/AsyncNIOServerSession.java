package com.freenote.app.server.core.nio.sessions;

import com.freenote.app.server.core.nio.ConnectionPipeline;
import com.freenote.app.server.core.nio.events.NIOEvent;
import com.freenote.app.server.core.nio.transport.NetworkSelector;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.model.ws.AsyncNIONetworkRequestData;
import com.freenote.app.server.model.ws.NetworkRequestData;
import lombok.Builder;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Builder
@Getter
public class AsyncNIOServerSession {
    private NetworkSelector selector;
    private AsynchronousServerSocketChannel asyncServer;
    private ConnectionPipeline pipeline;
    private static final Logger log = LogManager.getLogger(AsyncNIOServerSession.class);
    @Builder.Default
    private final Map<AsynchronousSocketChannel, AsyncNIONetworkRequestData> channelData = new ConcurrentHashMap<>();

    public void start() {
        asyncServer.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
                // Accept next connection
                asyncServer.accept(null, this);
                NetworkRequestData networkData = acceptConnection(socketChannel);
                // Edge-triggered: Only notified once when data arrives
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                socketChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer buffer) {
                        var networkData = channelData.get(socketChannel);
                        if (!readChannelData(networkData)) {
                            return;
                        }
                        pipeline.process(networkData);
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        try {
                            socketChannel.close();
                        } catch (IOException e) {
                            // Handle error
                        }
                    }
                });
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                // Handle error
            }
        });
    }

    public NetworkRequestData acceptConnection(AsynchronousSocketChannel asyncChannel) {
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
            NetworkRequestData networkData = new AsyncNIONetworkRequestData(asyncChannel, buffer);
            channelData.put(asyncChannel, (AsyncNIONetworkRequestData) networkData);
            return networkData;
        } catch (Exception e) {
            throw new AcceptConnectionException("Failed to accept connection", e);
        }
    }

    public void handleReadEvent(NIOEvent nioEvent) {
        try {
            SocketChannel channel = (SocketChannel) nioEvent.getChannel();
            var networkData = channelData.get(channel);
            if (networkData == null) {
                log.warn("No network data for channel {}", channel);
                return;
            }

            if (!readChannelData(networkData)) {
                return;
            }

            if (!pipeline.process(networkData)) {
                cleanupChannel(nioEvent, networkData);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean readChannelData(AsyncNIONetworkRequestData networkData) {
        try {
            if (networkData.readFromChannel() == -1) {
                pipeline.disconnect(networkData);
                return false;
            }
            networkData.prepareForRead();
            return true;
        } catch (IOException e) {
            log.warn("I/O error reading from channel {}", e.getMessage());
            pipeline.disconnect(networkData);
            return false;
        }
    }

    private void cleanupChannel(NIOEvent nioEvent, AsyncNIONetworkRequestData networkData) {
        SelectionKey key = nioEvent.getSelectionKey();
        if (key != null) {
            key.cancel();
        }
        SocketChannel channel = (SocketChannel) nioEvent.getChannel();
        channelData.remove(channel);
        try {
            networkData.close();
        } catch (IOException ignored) {
        }
        MetricUtils.decrementConcurrentUsers();
    }
}
