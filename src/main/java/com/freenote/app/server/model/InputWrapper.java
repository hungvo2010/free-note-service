package com.freenote.app.server.model;

import com.freenote.app.server.model.ws.CommonRequestObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InputWrapper {
    private Socket socket;
    private ByteBuffer channelBuffer;
    private SocketChannel socketChannel;
    private CommonRequestObject requestObject;
    private InputStream inputStream;

    public InputWrapper(Socket incomingSocket) {
        this.socket = incomingSocket;
    }

    public InputWrapper() {
    }

    public InputStream getInputStream() {
        if (inputStream != null) {
            return inputStream;
        }
        try {
            if (requestObject != null && requestObject.getSocket() != null) {
                return requestObject.getSocket().getInputStream();
            }
            if (socket != null) {
                return socket.getInputStream();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
