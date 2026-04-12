package com.freenote.app.server.core.connection;

import com.freenote.app.server.messages.WebSocketFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
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
import java.nio.channels.SocketChannel;

@Data
@Builder
public class WebSocketConnection {
    private InputStream inputStream;
    private InputWrapper inputWrapper;
    private OutputWrapper outputWrapper;
    private CommonRequestObject requestObject;
    private CommonResponseObject responseObject;
    private WebSocketFrame requestFrame;
    private WebSocketFrame responseFrame;
    private SocketChannel socketChannel;

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
        // Tự quản lý OutputWrapper nội bộ
        IOUtils.writeOutPut(outputWrapper.outputStream(), frame);
    }

    private void writeAsJsonTextFrame(CommonResponseObject obj) throws IOException {
        String json = JSONUtils.toJSONString(obj.getResponseData());
        writeFrame(FrameFactory.SERVER.createTextFrame(json));
    }

    public InputStream getInputStream() {
        return this.inputWrapper.getInputStream();
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
        return this.getInputWrapper().getSocket().getRemoteSocketAddress();
    }
}
