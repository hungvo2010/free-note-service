package com.freenote.app.server.core.v2;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.ws.CommonRequestObject;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static generated.URIHandlerRegistry.getInstanceByURI;

public class NIOIncomingConnectionHandlerImpl implements IncomingConnectionHandlerV2 {
    private static final Logger log = LogManager.getLogger(NIOIncomingConnectionHandlerImpl.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;

    public NIOIncomingConnectionHandlerImpl() {
        this.handshakeHandler = new AcceptHandshakeImpl();
        this.httpParser = new HttpParserImpl();
    }

    @Override
    public void handle(SocketChannel channel, ByteBuffer byteBuffer, HttpUpgradeRequest upgradeRequest) throws IOException {
        byteBuffer.clear();
        int read = channel.read(byteBuffer);
        if (read == -1) {
            channel.close();
            return;
        }
        var pathHandler = getPathHandler(upgradeRequest);
        var inputWrapper = buildInputWrapper(channel, upgradeRequest, byteBuffer);
        var outputWrapper = new OutputWrapper(channel.socket().getOutputStream());
        pathHandler.handle(inputWrapper, outputWrapper);
    }
    @Override
    public HttpUpgradeRequest handShake(SocketChannel channel, ByteBuffer byteBuffer) throws IOException {
        byteBuffer.clear();
        int read = channel.read(byteBuffer);
        if (read == -1) {
            channel.close();
            return null;
        }

        var upgradeRequest = this.httpParser.parse(byteBuffer);

        log.info("Received request: {}\n", upgradeRequest);

        var inputWrapper = buildInputWrapper(channel, upgradeRequest, byteBuffer);
        writeHandshakeResponse(upgradeRequest, inputWrapper);
        return upgradeRequest;
    }

    private URIHandler getPathHandler(HttpUpgradeRequest upgradeRequest) {
        var pathHandler = (URIHandler) (getInstanceByURI(upgradeRequest.getPath()));
        if (pathHandler == null) {
            log.warn("No handler found for URI: {}", upgradeRequest.getPath());
            throw new AcceptConnectionException("No handler for URI: " + upgradeRequest.getPath());
        }
        return pathHandler;
    }

    private InputWrapper buildInputWrapper(SocketChannel channel, HttpUpgradeRequest upgradeRequest, ByteBuffer byteBuffer) {
        var request = CommonRequestObject.builder()
                .origin(upgradeRequest.getOrigin())
                .socket(channel.socket())
                .build();
        return InputWrapper.builder()
                .socketChannel(channel)
                .channelBuffer(byteBuffer)
                .requestObject(request)
                .build();
    }

    private void writeHandshakeResponse(HttpUpgradeRequest request, InputWrapper inputWrapper) throws IOException {
        var handShakeResp = this.handshakeHandler.handle(request);
        var outputBytes = handShakeResp.toString().getBytes(StandardCharsets.UTF_8);
        var buffer = inputWrapper.getChannelBuffer();

        // 1. Xóa trạng thái cũ (đưa position về 0, limit về capacity)
        // để chuẩn bị ghi dữ liệu phản hồi vào buffer [3, 4]
        buffer.clear();

        // 2. Nạp dữ liệu mới vào buffer
        buffer.put(outputBytes);

        // 3. Chuyển buffer sang "chế độ đọc" (flip) để Channel có thể lấy dữ liệu ra [3, 7]
        buffer.flip();

        // 4. Ghi dữ liệu vào SocketChannel
        // Sử dụng vòng lặp để đảm bảo ghi hết dữ liệu nếu là non-blocking [5]
        var socketChannel = inputWrapper.getSocketChannel();
        while (buffer.hasRemaining()) {
            socketChannel.write(buffer);
        }
    }
}
