package com.freenote.app.server.model;

import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.model.http.HttpUpgradeResponse;
import com.freenote.app.server.util.IOUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@AllArgsConstructor
@Data
public class LegacyIOWrapper implements AutoCloseable {
    private Socket socket;

    public void sendHandshakeResponse(HttpUpgradeResponse handShakeResp) throws IOException {
        var output = socket.getOutputStream();
        output.write(handShakeResp.toString().getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    public void close() throws IOException {
        this.socket.close();
    }

    public void writeOutput(WebSocketFrame internalServerError) throws IOException {
        IOUtils.writeOutPut(this.socket.getOutputStream(), internalServerError);
    }
}
