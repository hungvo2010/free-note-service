package com.freedraw.legacy;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.core.legacy.DefaultLegacySessionBasedConnectionHandler;
import com.freenote.app.server.core.legacy.WebSocketServer;
import com.freenote.app.server.core.legacy.LegacyConnectionAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FreeNoteServer {
    private static final Logger log = LogManager.getLogger(FreeNoteServer.class);

    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        run(port);
    }

    public static void run(int port) throws Exception {
        WebSocketServer server = WebSocketServer.builder()
                .socketConfig(new ServerSocketConfig(port))
                .handler(new LegacyConnectionAdapter(new DefaultLegacySessionBasedConnectionHandler()))
                .build();
        server.start();
    }
}
