package com.freenote.app.server.core.model.connection;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.http.HttpUpgradeResponse;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.util.IOUtils;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;

@Data
@Builder
public class WebSocketSession {
    private final OutputWrapper outputWrapper;
    private final NetworkRequestData networkRequestData;

    public void sendHandshakeResponse(HttpUpgradeResponse handShakeResp) throws IOException {
        networkRequestData.write(handShakeResp.toRawBytes());
    }

    public Object getRemoteAddress() {
        return networkRequestData.getRemoteAddress();
    }

    public void writeResponse(WebSocketFrame frame) throws IOException {
        this.networkRequestData.write(IOUtils.frameToBytes(frame));
    }
}
