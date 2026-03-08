package com.freenote.app.server.core;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.TraceRequestData;
import com.freenote.app.server.model.ws.CommonRequestObject;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static generated.URIHandlerRegistry.getInstanceByURI;

public class IncomingSocketHandler implements IncomingConnectionHandler {
    private static final Logger log = LogManager.getLogger(IncomingSocketHandler.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;

    public IncomingSocketHandler(AcceptHandshakeHandler handshakeHandler, HttpParser httpParser) {
        this.handshakeHandler = handshakeHandler;
        this.httpParser = httpParser;
    }

    public IncomingSocketHandler() {
        this(new AcceptHandshakeImpl(), new HttpParserImpl());
    }

    @Override
    public void handle(Socket socket) throws IOException {
        try {
            serveConnection(socket);
        } catch (ClientDisconnectException | AcceptConnectionException connectionException) {
            handleClientDisconnect(socket, connectionException);
        } catch (Exception e) {
            handleError(socket, e);
        }
    }

    private void serveConnection(Socket socket) throws IOException {
        var upgradeRequest = parseRequest(socket);
        performHandshake(upgradeRequest, socket);
        
        routeToHandler(upgradeRequest, socket);
    }

    private HttpUpgradeRequest parseRequest(Socket socket) throws IOException {
        return httpParser.parse(socket.getInputStream());
    }

    private void performHandshake(HttpUpgradeRequest request, Socket socket) throws IOException {
        log.info("Performing handshake for: {}", request);
        var handShakeResp = this.handshakeHandler.handle(request);
        var output = socket.getOutputStream();
        output.write(handShakeResp.toString().getBytes(StandardCharsets.UTF_8));
        output.flush();

        if (!handShakeResp.getStatusCode().equals("101")) {
            throw new AcceptConnectionException("Handshake failed, connection not accepted");
        }
    }

    private void routeToHandler(HttpUpgradeRequest upgradeRequest, Socket socket) throws IOException {
        var pathHandler = getPathHandler(upgradeRequest);
        var inputWrapper = buildInputWrapper(socket, upgradeRequest);
        var outputWrapper = new OutputWrapper(socket.getOutputStream());

        while (!socket.isClosed()) {
            pathHandler.handle(inputWrapper, outputWrapper);
        }
    }

    private URIHandler getPathHandler(HttpUpgradeRequest upgradeRequest) {
        var pathHandler = (URIHandler) (getInstanceByURI(upgradeRequest.getPath()));
        if (pathHandler == null) {
            log.warn("No handler found for URI: {}", upgradeRequest.getPath());
            throw new AcceptConnectionException("No handler for URI: " + upgradeRequest.getPath());
        }
        return pathHandler;
    }

    private InputWrapper buildInputWrapper(Socket socket, HttpUpgradeRequest request) {
        var requestObject = CommonRequestObject.builder()
                .origin(request.getOrigin())
                .socket(socket)
                .build();
        requestObject.setRequestData(new TraceRequestData());

        var inputWrapper = new InputWrapper(socket);
        inputWrapper.setRequestObject(requestObject);

        return inputWrapper;
    }

    private void handleClientDisconnect(Socket socket, Exception e) throws IOException {
        log.error("Client disconnected => self closed: {}", e.getMessage());
        socket.close();
    }

    private void handleError(Socket socket, Exception e) throws IOException {
        log.error("Error handling socket: ", e);
        try {
            IOUtils.writeOutPut(
                    socket.getOutputStream(),
                    FrameFactory.SERVER.createTextFrame("Internal Server Error")
            );
        } catch (Exception ignore) {
        } finally {
            socket.close();
        }
    }
}
