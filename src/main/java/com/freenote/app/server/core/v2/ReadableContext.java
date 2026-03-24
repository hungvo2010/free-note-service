package com.freenote.app.server.core.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

@AllArgsConstructor
@Getter
@Builder
@Setter
public class ReadableContext {
    private SocketChannel channel;
    private SelectionKey key;
    private ByteBuffer byteBuffer;
    private TracingContext tracingContext;

    public void closeChannel() throws IOException {
        this.channel.close();
    }

    public void setState(ProcessingState processingState) {
        this.key.attach(processingState);
    }

    public SocketAddress getRemoteAddress() throws IOException {
        return this.channel.getRemoteAddress();
    }
}
