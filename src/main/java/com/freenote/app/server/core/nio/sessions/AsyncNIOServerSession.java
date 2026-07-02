package com.freenote.app.server.core.nio.sessions;

import com.freenote.app.server.core.nio.ConnectionPipeline;
import com.freenote.app.server.core.nio.transport.NetworkSelector;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.model.ws.AsyncNIONetworkRequestData;
import com.freenote.app.server.model.ws.NetworkRequestData;
import lombok.Builder;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Builder
@Getter
public class AsyncNIOServerSession {
    private NetworkSelector selector;
    private AsynchronousServerSocketChannel asyncServerChannel;
    private ConnectionPipeline pipeline;
    private static final Logger log = LogManager.getLogger(AsyncNIOServerSession.class);
    @Builder.Default
    private final Map<AsynchronousSocketChannel, AsyncNIONetworkRequestData> channelData = new ConcurrentHashMap<>();

    public void start() {
        asyncServerChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
                // Accept next connection
                asyncServerChannel.accept(null, this);
                var buffer = acceptConnection(socketChannel);
                // Edge-triggered: Only notified once when data arrives
                socketChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer buffer) {
                        var networkData = channelData.get(socketChannel);
                        if (!readChannelData(networkData)) {
                            return;
                        }
                        pipeline.process(networkData);
                        socketChannel.read(buffer, buffer, this);
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        try {
                            socketChannel.close();
                        } catch (IOException e) {
                            // Handle error
                            log.error("Failed to close socket channel", e);
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

    public ByteBuffer acceptConnection(AsynchronousSocketChannel asyncChannel) {
        try {
            ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
            NetworkRequestData networkData = new AsyncNIONetworkRequestData(asyncChannel, buffer);
            channelData.put(asyncChannel, (AsyncNIONetworkRequestData) networkData);
            return buffer;
        } catch (Exception e) {
            throw new AcceptConnectionException("Failed to accept connection", e);
        }
    }

    private boolean readChannelData(AsyncNIONetworkRequestData networkData) {
        try {
            networkData.prepareForRead();
            return true;
        } catch (Exception e) {
            log.error("I/O error reading from channel {}", e.getMessage());
            pipeline.disconnect(networkData);
            return false;
        }
    }
}
