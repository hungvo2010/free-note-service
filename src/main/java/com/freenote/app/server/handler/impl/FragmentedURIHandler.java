package com.freenote.app.server.handler.impl;

import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.factory.ServerFrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.LargeFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.util.FrameUtil;
import io.NoHeaderObjectOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

@URIHandlerImplementation("/update")
public class FragmentedURIHandler implements URIHandler {
    private static final Logger log = LogManager.getLogger(FragmentedURIHandler.class);

    @Override
    public boolean handle(InputStream inputStream, OutputStream outputStream) {
        if (inputStream == null || outputStream == null) throw new NullPointerException();

        try {
            var bytes = new byte[70000];
            int read = inputStream.read(bytes);
            if (read == -1) return false;
            var clientFrame = DataFrame.fromRawFrameBytes(Arrays.copyOfRange(bytes, 0, read));
            if (!clientFrame.isFin() && clientFrame.getOpcode() == FrameType.CONTINUATION.getOpCode()) {
                return continuationHandler(clientFrame, inputStream, outputStream);
            }
            var objectOutputStream = new NoHeaderObjectOutputStream(outputStream);
            objectOutputStream.writeObject(new ServerFrameFactory().createTextFrame(new String(FrameUtil.maskPayload(clientFrame.getPayloadData(), clientFrame.getMaskingKey()), "UTF-8")));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean continuationHandler(WebSocketFrame clientFrame, InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            int read = 0;
            LargeFrame largeFrame = new LargeFrame();
            largeFrame.addFragmentMessage((DataFrame) clientFrame);
            log.info("Frame content: {}", new String(clientFrame.getPayloadData()));
            do {
                var bytes = new byte[70000];
                read = inputStream.read(bytes);
                if (read != -1) {
                    largeFrame.addFragmentMessage(DataFrame.fromRawFrameBytes(Arrays.copyOfRange(bytes, 0, read)));
                }
            } while (!largeFrame.isComplete() || read != -1);
            var mergedFrame = largeFrame.getMergedFrame();
            var objectOutputStream = new NoHeaderObjectOutputStream(outputStream);
            objectOutputStream.writeObject(mergedFrame);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
