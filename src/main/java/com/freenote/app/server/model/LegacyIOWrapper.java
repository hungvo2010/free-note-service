package com.freenote.app.server.model;

import com.freenote.app.server.exceptions.ConnectionException;
import com.freenote.app.server.messages.WebSocketFrame;
import com.freenote.app.server.model.http.HttpUpgradeResponse;
import com.freenote.app.server.util.IOUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.net.Socket;

@AllArgsConstructor
@Data
public class LegacyIOWrapper implements AutoCloseable {
    private Socket socket;

    public void sendHandshakeResponse(HttpUpgradeResponse handShakeResp) throws ConnectionException {
        writeRawBytes(handShakeResp.toRawBytes());
    }

    private void writeRawBytes(byte[] data) throws ConnectionException {
        try {
            IOUtils.writeOutPut(socket.getOutputStream(), data);
        } catch (IOException e) {
            throw new ConnectionException("Cannot write to socket", e);
        }
    }


    public void close() throws IOException {
        try {
            this.socket.close();
        } catch (IOException e) {
        }
    }

    public void writeOutput(WebSocketFrame internalServerError) throws IOException {
        IOUtils.writeOutPut(this.socket.getOutputStream(), internalServerError);
    }
}
