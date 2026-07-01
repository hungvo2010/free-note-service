package com.freenote.app.server.core.nio.sessions;

import com.freenote.app.server.core.nio.ConnectionPipeline;
import com.freenote.app.server.core.nio.events.NIOEvent;
import com.freenote.app.server.core.nio.transport.NetworkSelector;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.exceptions.NIOReadException;
import com.freenote.app.server.model.ws.NIONetworkRequestData;
import lombok.Builder;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Builder
@Getter
public class NIOServerSession {
    private NetworkSelector selector;
    private ServerSocketChannel serverSocketChannel;
    private ConnectionPipeline pipeline;
    private static final Logger log = LogManager.getLogger(NIOServerSession.class);
    @Builder.Default
    private final Map<SocketChannel, NIONetworkRequestData> channelData = new ConcurrentHashMap<>();

    public void registerAcceptEvent() {
        try {
            serverSocketChannel.register(selector.getSelector(), SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException e) {
            throw new AcceptConnectionException("Failed to register accept event for server socket channel", e);
        }
    }

    public void acceptConnection(NIOEvent nioEvent) {
        try {
            SocketChannel client = serverSocketChannel.accept();
            if (client != null) {
                client.configureBlocking(false);
                ByteBuffer buffer = ByteBuffer.allocateDirect(2048);
                NIONetworkRequestData networkData = new NIONetworkRequestData(client, buffer);
                channelData.put(client, networkData);
                client.register(selector.getSelector(), SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            throw new AcceptConnectionException("Failed to accept connection", e);
        }
    }

    public void handleReadEvent(NIOEvent nioEvent) throws NIOReadException {
        try {
            SocketChannel channel = (SocketChannel) nioEvent.getChannel();
            NIONetworkRequestData networkData = channelData.get(channel);
            if (networkData == null) {
                log.warn("No network data for channel {}", channel);
                return;
            }

            if (!readChannelData(nioEvent, networkData)) {
                return;
            }

            if (!pipeline.process(networkData)) {
                cleanupChannel(nioEvent, networkData);
            }
        } catch (Exception e) {
            throw new NIOReadException(e);
        }
    }

    private boolean readChannelData(NIOEvent nioEvent, NIONetworkRequestData networkData) {
        try {
            if (networkData.readFromChannel() == -1) {
                pipeline.disconnect(networkData);
                cleanupChannel(nioEvent, networkData);
                return false;
            }
            networkData.prepareForRead();
            return true;
        } catch (IOException e) {
            log.warn("I/O error reading from channel {}", e.getMessage());
            pipeline.disconnect(networkData);
            cleanupChannel(nioEvent, networkData);
            return false;
        }
    }

    private void cleanupChannel(NIOEvent nioEvent, NIONetworkRequestData networkData) {
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
