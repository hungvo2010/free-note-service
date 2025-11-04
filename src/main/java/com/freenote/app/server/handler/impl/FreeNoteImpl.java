package com.freenote.app.server.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.application.CoreDraftProcessor;
import com.freenote.app.server.application.factory.ApplicationFrameFactory;
import com.freenote.app.server.application.factory.ServerApplicationFrameFactory;
import com.freenote.app.server.application.models.common.MessagePayload;
import com.freenote.app.server.application.models.common.WebSocketAPIResponse;
import com.freenote.app.server.application.models.request.DraftRequest;
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
            IOUtils.writeOutPut(outputStream, applicationFrameFactory.createApplicationFrame(WebSocketAPIResponse.UNEXPECTED_ERROR));
            return false;
        }
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputStream inputStream, OutputStream outputStream) {
        return false;
    }

    private void doApplicationLogic(byte[] rawBytes, OutputStream outputStream) {
        var dataFrame = DataFrame.fromRawFrameBytes(rawBytes);
        log.info("Received DataFrame opcode: {}", FrameType.fromOpCode(dataFrame.getOpcode()));
        log.info("Received DataFrame masking key length: {}", dataFrame.getMaskingKey().length);
        var rawPayload = FrameUtil.maskPayload(dataFrame.getPayloadData(), dataFrame.getMaskingKey());
        log.info("Received DataFrame content: {}", new String(rawPayload));
        var messagePayload = getMessagePayload(dataFrame);
        var response = handleClientMessage(messagePayload);
        var appResponse = applicationFrameFactory.createApplicationFrame(response);
        IOUtils.writeOutPut(outputStream, appResponse);
    }

    private MessagePayload handleClientMessage(MessagePayload messagePayload) {
        try {
            var actualPayload = messagePayload.getPayload();
            var draftRequest = convertToDraftRequest(actualPayload);
            return coreDraftProcessor.processDraft(draftRequest);
        } catch (Exception ex) {
            log.error("Error handling client message: {}", ex.getMessage());
            return DEFAULT_MESSAGE_PAYLOAD;
        }
    }

    private DraftRequest convertToDraftRequest(Object actualPayload) {
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
