package com.freenote.app.server.core.v2;

import lombok.Getter;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

@Getter
public class NetworkSelector {
    private Selector selector;

    public NetworkSelector(Selector selector) {
        this.selector = selector;
    }

    public boolean isHealthy() {
        return this.selector.isOpen();
    }

    public int select() throws IOException {
        return this.selector.select();
    }

    public Set<SelectionKey> getNewSelectionEvents() {
        return this.selector.selectedKeys();
    }
}
