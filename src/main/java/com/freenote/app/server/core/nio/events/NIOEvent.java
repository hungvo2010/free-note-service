package com.freenote.app.server.core.nio.events;

import lombok.Builder;
import lombok.Getter;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

@Builder
@Getter
public class NIOEvent {
    private SelectionKey selectionKey;

    public boolean isNewConnection() {
        return selectionKey.isAcceptable();
    }

    public boolean isNewMessage() {
        return selectionKey.isReadable();
    }

    public SelectableChannel getChannel() {
        return selectionKey.channel();
    }
}
