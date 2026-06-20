package com.freenote.app.server.model.ws;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NIONetworkRequestData implements NetworkRequestData {

    private final ByteBuffer byteBuffer;
    private final SocketChannel channel;
    private static final Logger log = LogManager.getLogger(NIONetworkRequestData.class);

    public NIONetworkRequestData(SocketChannel channel, ByteBuffer byteBuffer) {
        this.channel = channel;
        this.byteBuffer = byteBuffer;
    }

    @Override
    public WebSocketFrame buildRequestFrame() {
        return null;
    }

    @Override
    public WebSocketFrame buildResponseFrame() {
        return null;
    }

    @Override
    public byte[] readFrameBytes() {
        log.debug("Reading WebSocket frame from ByteBuffer (limit: {}, remaining: {})", byteBuffer.limit(), byteBuffer.remaining());
        try (InputStream inputStream = IOUtils.newInputStream(byteBuffer)) {
            return IOUtils.getFrameBytes(inputStream);
        } catch (IOException e) {
            return new byte[0];
        }
    }

    @Override
    public int read(byte[] data) {
        return 0;
    }

    @Override
    public void write(byte[] dataToWrite) {
        if (dataToWrite != null) {
            ByteBuffer buffer = ByteBuffer.wrap(dataToWrite);
            while (buffer.hasRemaining()) {
                try {
                    channel.write(buffer);
                } catch (IOException e) {
                    log.error("Error occurred while writing to SocketChannel", e);
                }
            }
        }
    }

    @Override
    public byte[] read() {
        return new byte[0];
    }

    @Override
    public void close() throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }

    @Override
    public boolean isClosed() {
        return channel != null && !channel.isOpen();
    }

    @Override
    public Object getRemoteAddress() {
        if (channel != null) {
            try {
                return channel.getRemoteAddress();
            } catch (IOException e) {
                log.warn("Failed to get remote address", e);
            }
        }
        return null;
    }

    public void prepareForRead() {
        if (byteBuffer.position() > 0) {
            byteBuffer.flip();
        }
    }
}
