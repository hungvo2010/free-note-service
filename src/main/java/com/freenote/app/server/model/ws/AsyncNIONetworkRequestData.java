package com.freenote.app.server.model.ws;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

public class AsyncNIONetworkRequestData implements NetworkRequestData {

    private final ByteBuffer byteBuffer;
    private final AsynchronousSocketChannel channel;
    private static final Logger log = LogManager.getLogger(AsyncNIONetworkRequestData.class);

    public AsyncNIONetworkRequestData(AsynchronousSocketChannel channel, ByteBuffer byteBuffer) {
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
        if (data == null || data.length == 0 || !byteBuffer.hasRemaining()) {
            return 0;
        }
        int len = Math.min(byteBuffer.remaining(), data.length);
        byteBuffer.get(data, 0, len);
        return len;
    }

    @Override
    public void write(byte[] dataToWrite) {
        if (dataToWrite != null) {
            ByteBuffer buffer = ByteBuffer.wrap(dataToWrite);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }
    }

    @Override
    public byte[] read() {
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);
        return data;
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

    /**
     * Prepares the internal buffer for reading by flipping it
     * (write-mode → read-mode). Called by the selector loop after
     * {@link #readFromChannel()}.
     */
    public void prepareForRead() {
        if (byteBuffer.position() > 0) {
            byteBuffer.flip();
        }
    }

    /**
     * Reads from the underlying channel into the internal buffer.
     * Clears the buffer first. Returns the number of bytes read, or -1 on EOF.
     * Called by the selector loop instead of {@code channel.read(byteBuffer)}.
     */
    public int readFromChannel() throws IOException {
        try {
            byteBuffer.clear();
            return channel.read(byteBuffer).get();
        } catch (Exception e) {
            log.error("Failed to read from channel", e);
            return -1;
        }
    }

    /**
     * Returns an OutputStream backed by {@link #write(byte[])}.
     * Used to construct {@code OutputWrapper} without accessing
     * {@code channel.socket().getOutputStream()}.
     */
    public OutputStream getOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                AsyncNIONetworkRequestData.this.write(new byte[]{(byte) b});
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                byte[] chunk = new byte[len];
                System.arraycopy(b, off, chunk, 0, len);
                AsyncNIONetworkRequestData.this.write(chunk);
            }
        };
    }
}
