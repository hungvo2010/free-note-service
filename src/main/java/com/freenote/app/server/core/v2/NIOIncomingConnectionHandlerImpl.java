package com.freenote.app.server.core.v2;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.core.IncomingSocketHandlerImpl;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class NIOIncomingConnectionHandlerImpl implements IncomingConnectionHandlerV2 {
    private static final Logger log = LogManager.getLogger(IncomingSocketHandlerImpl.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;

    public NIOIncomingConnectionHandlerImpl() {
        this.handshakeHandler = new AcceptHandshakeImpl();
        this.httpParser = new HttpParserImpl();
    }

    @Override
    public void handle(SocketChannel channel) throws IOException {
        channel.configureBlocking(false);
        var byteBuffer = ByteBuffer.allocateDirect(2048);

        var upgradeRequest = this.httpParser.parse(byteBuffer);

        log.info("Received request: {}\n", upgradeRequest);
        writeHandshakeResponse(upgradeRequest, channel.socket().getOutputStream());

    }

    private void writeHandshakeResponse(HttpUpgradeRequest request, OutputStream output) throws IOException {
        var handShakeResp = this.handshakeHandler.handle(request);
        output.write(handShakeResp.toString().getBytes(StandardCharsets.UTF_8));
        output.flush();
        if (!handShakeResp.getStatusCode().equals("101")) {
            throw new AcceptConnectionException("Handshake failed, connection not accepted");
        }
    }

//    @Override
//    public void handle(Socket incomingSocket) throws IOException {
//        var input = incomingSocket.getInputStream();
//        var output = incomingSocket.getOutputStream();
//        try {
//            log.info("Serving incoming socket: {}", incomingSocket.getPort());
//            var upgradeRequest = this.httpParser.parse(input);
//
//            log.info("Received request: {}\n", upgradeRequest);
//            doHandShake(upgradeRequest, output);
//
//            var inputWrapper = buildInputWrapper(incomingSocket, upgradeRequest);
//            var pathHandler = getPathHandler(upgradeRequest);
//
//            while (!incomingSocket.isClosed()) { // todo: not correct due to incoming socket will not be closed after client disconnects
////                log.warn("todo: not correct due to incoming socket will not be closed after client disconnects");
//                pathHandler.handle(inputWrapper, output);
//            }
//
//        } catch (ClientDisconnectException | AcceptConnectionException connectionException) {
//            log.error("Client disconnected => self closed: {}", connectionException.getMessage());
//            incomingSocket.close();
//        } catch (Exception e) {
//            log.error("Error handling socket: ", e);
//            IOUtils.writeOutPut(
//                    output,
//                    FrameFactory.SERVER.createTextFrame("Internal Server Error")
//            );
//        }
//    }
}
