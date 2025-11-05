package com.freenote.app.server.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.application.CoreDraftProcessor;
import com.freenote.app.server.application.factory.ApplicationFrameFactory;
import com.freenote.app.server.application.factory.ServerApplicationFrameFactory;
import com.freenote.app.server.application.models.common.MessagePayload;
import com.freenote.app.server.application.models.core.RoomManager;
import com.freenote.app.server.application.models.request.DraftRequest;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.exceptions.MessagePayloadParsingException;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@URIHandlerImplementation("/freeNote")
public class FreeNoteImpl implements URIHandler {
    private static final Logger log = LogManager.getLogger(FreeNoteImpl.class);
    private static final MessagePayload DEFAULT_MESSAGE_PAYLOAD = new MessagePayload();
    private final ObjectMapper objMapper = new ObjectMapper();
    private final CoreDraftProcessor coreDraftProcessor = new CoreDraftProcessor();
    private final ApplicationFrameFactory applicationFrameFactory = new ServerApplicationFrameFactory();

    @Override
    public boolean handle(InputStream inputStream, OutputStream outputStream) {
        try {
            if (inputStream.available() == 0) {
                return true; // No data, don't block
            }
            log.info("FreeNoteImpl.handle() called");
            var rawBytes = IOUtils.getRawBytes(inputStream);
            doApplicationLogic(rawBytes, outputStream);
            return true;
        } catch (Exception e) {
            log.error("Error handling input stream: {}", e.getMessage());
            throw new ClientDisconnectException("Client disconnected or error occurred", e);
        }
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputStream inputStream, OutputStream outputStream) {
        return false;
    }

    private void doApplicationLogic(byte[] rawBytes, OutputStream outputStream) throws IOException {
        try {
            var dataFrame = DataFrame.fromRawFrameBytes(rawBytes);
            log.info("Received DataFrame opcode: {}", FrameType.fromOpCode(dataFrame.getOpcode()));
            if (dataFrame.getOpcode() == FrameType.CLOSE.getOpCode()) {
                log.info("Received CLOSE frame. No further processing.");
                throw new ClientDisconnectException("Client sent CLOSE frame");
            }
            log.info("Received DataFrame masking key length: {}", dataFrame.getMaskingKey().length);
            var rawPayload = FrameUtil.maskPayload(dataFrame.getPayloadData(), dataFrame.getMaskingKey());
            log.info("Received DataFrame content: {}", new String(rawPayload));
            var messagePayload = getMessagePayload(dataFrame);
            var draftRequest = convertToDraftRequest(messagePayload);
            RoomManager.getInstance().addConnection(draftRequest.getDraftId(), outputStream);
            var response = handleClientMessage(messagePayload);
            var appResponse = applicationFrameFactory.createApplicationFrame(response);
            IOUtils.writeOutPut(outputStream, appResponse);
        } catch (MessagePayloadParsingException ex) {
            log.error("Failed to parse MessagePayload: {}", ex.getMessage());
            var errorResponse = applicationFrameFactory.createApplicationFrame(DEFAULT_MESSAGE_PAYLOAD);
            IOUtils.writeOutPut(outputStream, errorResponse);
        } catch (Exception ex) {
            log.error("Error in application logic: {}", ex.getMessage());
            throw ex;
        }
    }

    private MessagePayload handleClientMessage(MessagePayload messagePayload) {
        try {
            return coreDraftProcessor.processDraft(convertToDraftRequest(messagePayload));
        } catch (Exception ex) {
            log.error("Error handling client message: {}", ex.getMessage());
            return DEFAULT_MESSAGE_PAYLOAD;
        }
    }

    private void attachRequestMetaData(DraftRequest draftRequest) {
    }

    private DraftRequest convertToDraftRequest(MessagePayload messagePayload) {
        var actualPayload = messagePayload.getPayload();
        return objMapper.convertValue(actualPayload, DraftRequest.class);
    }

    private MessagePayload getMessagePayload(DataFrame dataFrame) {
        var rawPayload = FrameUtil.maskPayload(dataFrame.getPayloadData(), dataFrame.getMaskingKey());
        try {
            return objMapper.readValue(rawPayload, MessagePayload.class);
        } catch (IOException e) {
            log.error("Error parsing MessagePayload from DataFrame", e.getCause());
            throw new MessagePayloadParsingException("Error parsing MessagePayload from DataFrame", e);
        }
    }
}
