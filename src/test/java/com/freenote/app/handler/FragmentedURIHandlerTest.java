package com.freenote.app.handler;

import com.freenote.app.server.frames.factory.ClientFrameFactory;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.handler.impl.FragmentedURIEndpointHandlerImpl;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FragmentedURIHandlerTest {
    private static final Logger log = LogManager.getLogger(FragmentedURIHandlerTest.class);
    private final FrameFactory clientFactory = new ClientFrameFactory();
    private final FragmentedURIEndpointHandlerImpl handler = new FragmentedURIEndpointHandlerImpl();

    @Test
    void givenMockEOFInputStream_whenParseToContinuationFrame_thenSuccess() throws IOException {
        var mockOutputStream = mock(OutputStream.class);
        var wrapper = mock(InputWrapper.class);
        when(wrapper.getInputStream().read(any(byte[].class))).thenReturn(-1);
        assertFalse(handler.handle(wrapper, new OutputWrapper(mockOutputStream)));
    }

    @Test
    void givenRawDataBytes_whenParseToContinuationFrame_thenSuccess() throws IOException, InterruptedException {
        var firstFrame = clientFactory.createNonFinalFrame(FrameType.TEXT.getOpCode(), "Hello ".getBytes());
        var continuationFrame = clientFactory.createContinuationFrame("World".getBytes());
        var finalFrame = clientFactory.createTextFrame("!");

        var pipedOutputStream = new PipedOutputStream();
        var inputStream = new PipedInputStream(pipedOutputStream); // for internal java piped network mechanism, not network

        IOUtils.writeOutPut(pipedOutputStream, firstFrame);
        IOUtils.writeOutPut(pipedOutputStream, continuationFrame);


        var outputStream = new ByteArrayOutputStream();
        var atomicBoolean = new AtomicBoolean(false);

        Thread newThread = new Thread(() -> {
            try {
                var result = handler.handle(new InputWrapper(mock(Socket.class)), new OutputWrapper(outputStream));
                log.info("Result: {}", result);
                atomicBoolean.set(result);
            } catch (Throwable t) {
                log.error("Error while handling URI", t);
                atomicBoolean.set(false);
            }
        });
        newThread.start();

        log.info("send last frame");
        IOUtils.writeOutPut(pipedOutputStream, finalFrame);

        newThread.join(); // waiting for the thread to finish

        assertTrue(atomicBoolean.get());
    }

    @Test
    void givenFragmentedHandler_whenSendNonFinalNotContinuationFrame_thenReturnFalse() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> clientFactory.createNonFinalFrame(FrameType.CONTINUATION.getOpCode(), "Hello ".getBytes()));
        var someFrame = DataFrame.fromRawFrameBytes(new byte[]{0x00, 0x00});
        var pipedOutputStream = new PipedOutputStream();
        var inputStream = new PipedInputStream(pipedOutputStream);

        IOUtils.writeOutPut(pipedOutputStream, someFrame);

        var outputStream = new ByteArrayOutputStream();
        var result = handler.handle(new InputWrapper(mock(Socket.class)), new OutputWrapper(outputStream));

        assertFalse(result);
    }

    @Test
    void givenFragmentedHandler_whenSendFinalFrame_thenShouldReceivedValidEcho() throws IOException {

        var someFrame = clientFactory.createTextFrame("Hello World");
        var pipedOutputStream = new PipedOutputStream();
        var inputStream = new PipedInputStream(pipedOutputStream);

        IOUtils.writeOutPut(pipedOutputStream, someFrame);

        var outputStream = new ByteArrayOutputStream();
        var result = handler.handle(new InputWrapper(mock(Socket.class)), new OutputWrapper(outputStream));

        assertTrue(result);
    }

    @Test
    void givenFragmentedHandler_whenSimulateOutputStreamException_thenShouldReceivedValidEcho() throws IOException {
        var someFrame = clientFactory.createTextFrame("Hello World");

        var pipedOutputStream = new PipedOutputStream();
        var inputStream = new PipedInputStream(pipedOutputStream);

        IOUtils.writeOutPut(pipedOutputStream, someFrame);

        var outputStream = mock(OutputStream.class);
        doThrow(new IOException("Simulated write error")).when(outputStream).write(any(byte[].class));

        var result = handler.handle(new InputWrapper(mock(Socket.class)), new OutputWrapper(outputStream));

        assertFalse(result);
    }

}
