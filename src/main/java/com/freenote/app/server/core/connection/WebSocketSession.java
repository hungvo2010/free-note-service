package com.freenote.app.server.core.connection;

import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.http.HttpUpgradeResponse;
import com.freenote.app.server.util.IOUtils;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;

@Data
@Builder
public class WebSocketSession {
    private final Socket socket;
    private final SocketChannel socketChannel;
    private final InputWrapper inputWrapper;
    private final OutputWrapper outputWrapper;

    public void sendHandshakeResponse(HttpUpgradeResponse handShakeResp) throws IOException {
        IOUtils.writeOutPut(outputWrapper.outputStream(), handShakeResp.toRawBytes());
    }

    public Object getRemoteAddress() {
        if (socket != null) {
            return socket.getRemoteSocketAddress();
        }
        try {
            if (socketChannel != null) {
                return socketChannel.getRemoteAddress();
            }
        } catch (IOException e) {
            // ignore
        }
        return null;
    }
}
