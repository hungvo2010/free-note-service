package com.freenote.app.server.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.application.CoreDraftProcessor;
import com.freenote.app.server.application.factory.ApplicationFrameFactory;
import com.freenote.app.server.application.factory.ServerApplicationFrameFactory;
import com.freenote.app.server.application.models.common.MessagePayload;
import com.freenote.app.server.application.models.core.Room;
import com.freenote.app.server.application.models.core.RoomManager;
import com.freenote.app.server.application.models.request.DraftRequest;
import com.freenote.app.server.connections.Connection;
import com.freenote.app.server.connections.WebSocketConnection;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.exceptions.MessagePayloadParsingException;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@URIHandlerImplementation("/freeNote")
public class FreeNoteImpl extends CommonHandlerImpl {
    private static final Logger log = LogManager.getLogger(FreeNoteImpl.class);
    private static final MessagePayload DEFAULT_MESSAGE_PAYLOAD = new MessagePayload();
    private final ObjectMapper objMapper = new ObjectMapper();
    private final CoreDraftProcessor coreDraftProcessor = new CoreDraftProcessor();
    private final ApplicationFrameFactory applicationFrameFactory = new ServerApplicationFrameFactory();
    private final RoomManager roomManager = RoomManager.getInstance();

    @Override
    public boolean handle(InputWrapper inputWrapper, OutputStream outputStream) throws IOException {
        try {
            var inputStream = inputWrapper.getInputStream();
            if (inputStream.available() == 0) {
                return true; // No data, don't block
            }
            log.info("FreeNoteImpl.handle() called");

            var rawBytes = IOUtils.getRawBytes(inputStream);

            var draftRequest = extractDraftRequest(rawBytes);
            var clientResponse = doApplicationLogic(draftRequest);

            IOUtils.writeOutPut(outputStream, clientResponse);
            broadcastMessage(draftRequest.getDraftId(), new Connection(outputStream), clientResponse);

            return true;

        } catch (ClientDisconnectException e) {
            log.error("Client disconnected: {}", e.getMessage());
            removeConnectionByInputStream(outputStream);
            throw e;
        } catch (Exception e) {
            log.error("Error handling input stream: {}", e);
            throw e;
        }

    }

    private void removeConnectionByInputStream(OutputStream outputStream) {
        roomManager.removeConnectionByInputStream(outputStream);
    }

    private void removeConnection(Room targetRoom, Connection newConnection) {
        targetRoom.remove(newConnection);
    }

    private WebSocketFrame doApplicationLogic(DraftRequest draftRequest) {
        try {
            var responsePayload = handleClientMessage(draftRequest);
            return applicationFrameFactory.createApplicationFrame(responsePayload);
        } catch (MessagePayloadParsingException ex) {
            log.error("Failed to parse MessagePayload: {}", ex.getMessage());
            return applicationFrameFactory.createApplicationFrame(DEFAULT_MESSAGE_PAYLOAD);
        } catch (Exception ex) {
            log.error("Error in application logic: {}", ex.getMessage());
            throw ex;
        }
    }

    private DraftRequest extractDraftRequest(byte[] rawBytes) {
        var dataFrame = DataFrame.fromRawFrameBytes(rawBytes);
        log.info("Received DataFrame opcode: {}", FrameType.fromOpCode(dataFrame.getOpcode()));
        if (dataFrame.getOpcode() == FrameType.CLOSE.getOpCode()) {
            log.warn("Received CLOSE frame. No further processing.");
            throw new ClientDisconnectException("Client sent CLOSE frame");
        }

        log.info("Received DataFrame masking key length: {}", dataFrame.getMaskingKey().length);
        var rawPayload = FrameUtil.maskPayload(dataFrame.getPayloadData(), dataFrame.getMaskingKey());
        log.info("Received DataFrame content: {}", new String(rawPayload));
        var messagePayload = getMessagePayload(new String(rawPayload));
        return convertToDraftRequest(messagePayload);
    }

    private void broadcastMessage(String roomId, Connection newConnection, WebSocketFrame clientResponse) {
        var targetRoom = roomManager.getRoomById(roomId);
        try {
            targetRoom.addMember(newConnection);
            var connectionsToBroadcast = targetRoom.getConnectionsInRoomToBroadcast(List.of(newConnection));
            targetRoom.broadCastMessage(connectionsToBroadcast, clientResponse);
        } catch (Exception e) {
            removeConnection(targetRoom, newConnection);
        }
    }

    private MessagePayload handleClientMessage(DraftRequest draftRequest) {
        try {
            return coreDraftProcessor.processDraft(draftRequest);
        } catch (Exception ex) {
            log.error("Error handling client message: ", ex);
            return DEFAULT_MESSAGE_PAYLOAD;
        }
    }

    private DraftRequest convertToDraftRequest(MessagePayload messagePayload) {
        var actualPayload = messagePayload.getPayload();
        return objMapper.convertValue(actualPayload, DraftRequest.class);
    }

    private MessagePayload getMessagePayload(String rawPayload) {
        try {
            return objMapper.readValue(rawPayload, MessagePayload.class);
        } catch (IOException e) {
            log.error("Error parsing MessagePayload from DataFrame", e.getCause());
            throw new MessagePayloadParsingException("Error parsing MessagePayload from DataFrame", e);
        }
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputWrapper inputStream, OutputStream outputStream) {
        return false;
    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote) {
        removeConnectionByInputStream(webSocketConnection.getOutputStream());
        throw new ClientDisconnectException("Client sent CLOSE frame");

    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Exception throwable) {
        log.error("Error handling input stream: {}", e);
        throw throwable;
    }
}
