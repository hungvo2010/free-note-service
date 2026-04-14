package com.freenote.app.server.core.connection;

import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.messages.ws.WebSocketFrame;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.ws.CommonRequestObject;
import com.freenote.app.server.model.ws.CommonResponseObject;
import com.freenote.app.server.util.IOUtils;
import com.freenote.app.server.util.JSONUtils;
import lombok.Builder;
import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;

@Data
@Builder
public class WebSocketConnection {
    private final WebSocketSession session;
    private CommonRequestObject requestObject;
    private CommonResponseObject responseObject;
    private WebSocketFrame requestFrame;
    private WebSocketFrame responseFrame;

    public void sendCurrentResponse() throws IOException {
        if (hasResponseFrame()) {
            writeFrame(responseFrame);
        } else if (hasResponseObject()) {
            writeAsJsonTextFrame(responseObject);
        }
    }

    public boolean hasResponseFrame() {
        return responseFrame != null;
    }

    public boolean hasResponseObject() {
        return responseObject != null;
    }

    private void writeFrame(WebSocketFrame frame) throws IOException {
        IOUtils.writeOutPut(getOutputStream(), frame);
    }

    private void writeAsJsonTextFrame(CommonResponseObject obj) throws IOException {
        String json = JSONUtils.toJSONString(obj.getResponseData());
        writeFrame(FrameFactory.SERVER.createTextFrame(json));
    }

    public InputStream getInputStream() {
        return session.getInputWrapper().getInputStream();
    }

    public OutputStream getOutputStream() {
        return session.getOutputWrapper().outputStream();
    }

    public SocketChannel getSocketChannel() {
        return session.getSocketChannel();
    }

    public void sendText(String message) {
        setResponseFrame(FrameFactory.SERVER.createTextFrame(message));
    }

    public byte[] getPayloadBytes() throws IOException {
        byte[] dataToWrite = new byte[0];
        if (hasResponseFrame()) {
            dataToWrite = getFromResponseFrame();
        } else if (hasResponseObject()) {
            dataToWrite = getFromResponseObject();
        }
        return dataToWrite;
    }


    private byte[] getFromResponseObject() throws IOException {
        byte[] dataToWrite;
        try (var baos = new ByteArrayOutputStream()) {
            IOUtils.writeOutPut(
                    baos,
                    FrameFactory.SERVER.createTextFrame(
                            JSONUtils.toJSONString(getResponseObject().getResponseData()
                            )));
            dataToWrite = baos.toByteArray();
        }
        return dataToWrite;
    }

    private byte[] getFromResponseFrame() throws IOException {
        byte[] dataToWrite;
        try (var baos = new ByteArrayOutputStream()) {
            IOUtils.writeOutPut(baos, getResponseFrame());
            dataToWrite = baos.toByteArray();
        }
        return dataToWrite;
    }

    public Object getRemoteAddress() {
        return session.getRemoteAddress();
    }

    public static WebSocketConnection from(InputWrapper inputWrapper, OutputWrapper outputWrapper) {
        var session = WebSocketSession.builder()
                .inputWrapper(inputWrapper)
                .outputWrapper(outputWrapper)
                .socket(inputWrapper.getSocket())
                .socketChannel(inputWrapper.getSocketChannel())
                .build();
        return WebSocketConnection.builder()
                .session(session)
                .build();
    }
}
