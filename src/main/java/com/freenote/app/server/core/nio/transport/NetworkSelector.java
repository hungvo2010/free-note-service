package com.freenote.app.server.core.nio.transport;

import com.freenote.app.server.core.nio.events.NIOEvent;
import lombok.Getter;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Getter
public class NetworkSelector {
    private final Selector selector;

    public NetworkSelector(Selector selector) {
        this.selector = selector;
    }

    public boolean isHealthy() {
        return this.selector.isOpen();
    }

    public int select() throws IOException {
        return this.selector.select();
    }

    public Set<NIOEvent> getNewSelectionEvents() {
        var selectedKeys = this.selector.selectedKeys();
        var setOfEvents = new HashSet<NIOEvent>();

        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
        while (keyIterator.hasNext()) {
            SelectionKey key = keyIterator.next();
            var nioEvent = NIOEvent.builder().selectionKey(key).build();
            setOfEvents.add(nioEvent);
            keyIterator.remove();
        }
        return setOfEvents;
        }
}
