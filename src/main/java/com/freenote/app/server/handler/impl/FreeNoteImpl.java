package com.freenote.app.server.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.application.CoreDraftProcessor;
import com.freenote.app.server.application.factory.ServerApplicationFrameFactory;
import com.freenote.app.server.application.models.MessagePayload;
import com.freenote.app.server.application.models.request.DraftRequest;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.handler.URIHandler;
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
    private CoreDraftProcessor coreDraftProcessor;

    @Override
    public boolean handle(InputStream inputStream, OutputStream outputStream) {
        try {
            if (inputStream.available() == 0) {
                return true; // No data, don't block
            }
            var rawBytes = IOUtils.getRawBytes(inputStream);
            doApplicationLogic(rawBytes, outputStream);
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputStream inputStream, OutputStream outputStream) throws IOException {
        return false;
    }

    private void doApplicationLogic(byte[] rawBytes, OutputStream outputStream) throws IOException {
        var dataFrame = DataFrame.fromRawFrameBytes(rawBytes);
        var messagePayload = getMessagePayload(dataFrame);
        var response = handleClientMessage(messagePayload);
        var appResponse = new ServerApplicationFrameFactory().createApplicationFrame(response);
        IOUtils.writeOutPut(outputStream, appResponse);
    }

    private MessagePayload handleClientMessage(MessagePayload messagePayload) {
        try {
            var actualPayload = messagePayload.getPayload();
            var draftRequest = convertToDraftRequest(actualPayload);
            return coreDraftProcessor.processDraft(draftRequest);
        } catch (Exception ex) {
            log.error("Error handling client message", ex);
            return DEFAULT_MESSAGE_PAYLOAD;
        }
    }

    private DraftRequest convertToDraftRequest(Object actualPayload) {
        return objMapper.convertValue(actualPayload, DraftRequest.class);
    }

    private MessagePayload getMessagePayload(DataFrame dataFrame) {
        var payloadData = dataFrame.getPayloadData();
        try {
            return objMapper.readValue(payloadData, MessagePayload.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
