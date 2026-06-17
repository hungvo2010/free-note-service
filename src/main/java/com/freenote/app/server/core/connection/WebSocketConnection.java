package com.freenote.app.server.core.connection;

import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.ws.AppRequestData;
import com.freenote.app.server.model.ws.AppResponseData;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.util.IOUtils;
import com.freenote.app.server.util.JSONUtils;
import lombok.Builder;
import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;

@Data
@Builder
public class WebSocketConnection {
    private final WebSocketSession session;
    private AppRequestData appRequestData;
    private AppResponseData appResponseData;
    private WebSocketFrame requestFrame;
    private WebSocketFrame responseFrame;

    public void sendCurrentResponse() throws IOException {
        if (hasResponseFrame()) {
            writeFrame(responseFrame);
        } else if (hasResponseData()) {
            writeAsJsonTextFrame(appResponseData);
        }
    }

    public boolean hasResponseFrame() {
        return responseFrame != null;
    }

    public boolean hasResponseData() {
        return appResponseData != null;
    }

    private void writeFrame(WebSocketFrame frame) throws IOException {
        IOUtils.writeOutPut(getOutputStream(), frame);
    }

    private void writeAsJsonTextFrame(AppResponseData obj) throws IOException {
        String json = JSONUtils.toJSONString(obj);
        writeFrame(FrameFactory.SERVER.createTextFrame(json));
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
        } else if (hasResponseData()) {
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
                            JSONUtils.toJSONString(getAppResponseData()
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

    public static WebSocketConnection from(NetworkRequestData requestData, OutputWrapper outputWrapper) {
        var session = WebSocketSession.builder()
                .networkRequestData(requestData).build();
        return WebSocketConnection.builder()
                .session(session)
                .build();
    }
}
