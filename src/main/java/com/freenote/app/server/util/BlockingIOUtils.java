package com.freenote.app.server.util;

import lombok.experimental.UtilityClass;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.Socket;

@UtilityClass
public class BlockingIOUtils {
    public static boolean nonBlockingRead(Socket socket) throws IOException {
        try {
            var pushBackStream = new PushbackInputStream(socket.getInputStream());
            int b = pushBackStream.read();
            if (b < 0) return false;
            pushBackStream.unread(b); // wrong, due to use internal buffer, will not affect original input stream
            return true;
        } catch (Exception e) {
            return false; // no data yet, not an error
        }
    }

    public static boolean isAvailable(Socket socket) throws IOException {
        if (socket instanceof SSLSocket) {
            return nonBlockingRead(socket);
        }
        return socket.getInputStream().available() > 0;
    }
}
