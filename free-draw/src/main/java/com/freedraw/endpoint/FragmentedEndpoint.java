package com.freedraw.endpoint;

import com.freenote.annotations.WebSocketEndpoint;
import com.freenote.app.server.endpoints.URIEndpointHandler;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.LargeFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.model.ws.NetworkResponseData;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@WebSocketEndpoint("/update")
public class FragmentedEndpoint implements URIEndpointHandler {
    private static final Logger log = LogManager.getLogger(FragmentedEndpoint.class);
    private final FrameFactory frameFactory = new FrameFactory.ServerFrameFactory();

    @Override
    public boolean handle(NetworkRequestData networkRequestData, NetworkResponseData responseData) {
        try {
            var bytes = new byte[70000];
            int read = networkRequestData.read(bytes);
            if (read == -1) {
                log.info("End of stream reached");
                return false;
            }
            var clientFrames = readOneOrMultipleFrames(Arrays.copyOfRange(bytes, 0, read));
            var clientFrame = clientFrames.get(0);
            if (!clientFrame.isFin() && clientFrame.getOpcode() != FrameType.CONTINUATION.getOpCode()) {
                log.info("Received non-final frame. Continuation expected.");
                return continuationHandler(clientFrames, networkRequestData, responseData);
            } else if (clientFrame.getOpcode() == FrameType.CONTINUATION.getOpCode()) {
                log.info("Received continuation frame without initial fragmented frame. Ignoring.");
                return false;
            }
            IOUtils.writeOutPut(responseData.outputStream(), frameFactory.createTextFrame(
                    new String(
                            WebSocketFrame.applyMask(
                                    clientFrame.getPayloadData(),
                                    clientFrame.getMaskingKey()
                            ),
                            StandardCharsets.UTF_8)
            ));
            return true;
        } catch (IOException e) {
            log.error("Error handling input stream", e);
            return false;
        }
    }

    private List<WebSocketFrame> readOneOrMultipleFrames(byte[] bytes) {
        int byteRead = 0;
        var allFrames = new ArrayList<WebSocketFrame>();
        while (byteRead < bytes.length) {
            var frame = DataFrame.fromRawFrameBytes(Arrays.copyOfRange(bytes, byteRead, bytes.length));
            allFrames.add(frame);
            byteRead += frame.getTotalFrameLength();
        }
        return allFrames;
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrames, NetworkRequestData networkRequestData, NetworkResponseData responseData) throws IOException {
        LargeFrame largeFrame = new LargeFrame();
        try {
            int read;
            for (var clientFrame : clientFrames) {
                largeFrame.addFragmentMessage((DataFrame) clientFrame);
                log.info("Frame content: {}", new String(WebSocketFrame.applyMask(clientFrame.getPayloadData(), clientFrame.getMaskingKey()), StandardCharsets.UTF_8));
            }
            do {
                log.info("Reading more data...");
                var bytes = new byte[70000];
                read = networkRequestData.read(bytes);
                log.info("Read {} bytes", read);
                if (read != -1) {
                    largeFrame.addFragmentMessage(DataFrame.fromRawFrameBytes(Arrays.copyOfRange(bytes, 0, read)));
                }
            } while (!largeFrame.isComplete() || read == -1);
            log.info("Large frame is complete");
            var mergedFrame = largeFrame.getMergedFrame();
            IOUtils.writeOutPut(responseData.outputStream(), mergedFrame);
            return true;
        } catch (IOException e) {
            var mergedFrame = largeFrame.getMergedFrame();
            var content = new String(mergedFrame.getPayloadData(), StandardCharsets.UTF_8);
            log.error("Error during continuation handling. Partial content: {}", content, e);
            IOUtils.writeOutPut(responseData.outputStream(), mergedFrame);
            return false;
        }
    }
}
