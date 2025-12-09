package com.freenote.app.server.connections;

import com.freenote.app.server.application.models.request.core.RequestObject;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.util.IOUtils;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Data
@Builder
public class WebSocketConnection<T> {
    private InputStream inputStream;
    private OutputStream outputStream;
    private Socket socket;
    private RequestObject requestObject;
    private T response;
    private WebSocketFrame responseFrame;

    public void send(String message) {
        try {
            IOUtils.writeOutPut(this.outputStream, FrameFactory.SERVER.createTextFrame(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public T getResponse() {
        return this.response;
    }

    public void setFrame(WebSocketFrame pongFrame) {
        this.responseFrame = pongFrame;
    }
}
