package com.freedraw;

import com.freenote.app.server.core.v2.NIOIncomingSocketHandler;
import com.freenote.app.server.core.v2.WebSocketServerV2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModernEchoServer {
    private static final Logger log = LogManager.getLogger(ModernEchoServer.class);

    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8189;
        run(port);
    }

    public static void run(int port) throws Exception {
        WebSocketServerV2 server = WebSocketServerV2.builder()
                .port(port)
                .useSSL(false)
                .handler(new NIOIncomingSocketHandler())
                .build();
        server.start();
    }
}
