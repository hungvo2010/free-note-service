package com.freenote.app.server.frames;

import com.freenote.app.server.factory.FrameFactory;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;

import java.util.List;

public class EnhancedFrameFactory {
    private final FrameFactory delegateFactory;
    private final MessageFragmenter fragmenter;
    private final FragmentedMessageHandler messageHandler;

    public EnhancedFrameFactory(FrameFactory delegateFactory, int maxFragmentSize) {
        this.delegateFactory = delegateFactory;
        this.fragmenter = new MessageFragmenter(delegateFactory, maxFragmentSize);
        this.messageHandler = new FragmentedMessageHandler();
    }

    /**
     * Create text frame, automatically fragmenting if needed
     *
     * @param text The text message
     * @return Single frame if small enough, or list of fragments
     */
    public List<DataFrame> createTextFrameWithFragmentation(String text) {
        return fragmenter.fragmentTextMessage(text);
    }

    /**
     * Create binary frame, automatically fragmenting if needed
     *
     * @param data The binary data
     * @return Single frame if small enough, or list of fragments
     */
    public List<DataFrame> createBinaryFrameWithFragmentation(byte[] data) {
        return fragmenter.fragmentBinaryMessage(data);
    }

    /**
     * Process incoming frame and handle reassembly
     *
     * @param frame Incoming frame (could be fragment)
     * @return Complete message if fragmentation is complete, null if still collecting
     */
    public DataFrame processIncomingFrame(WebSocketFrame frame) {
        return messageHandler.processFrame(frame);
    }

    /**
     * Create continuation frame using existing DataFrame
     * This fixes the bug in ClientFrameFactory where it uses ControlFrame
     */
    public WebSocketFrame createContinuationFrame(byte[] data, boolean isFinal) {
        DataFrame continuationFrame = new DataFrame(FrameType.CONTINUATION.getOpCode(), data);
        continuationFrame.setFin(isFinal);

        // Reuse factory logic for masking
        if (isClientFactory()) {
            continuationFrame.setMasked(true);
            byte[] maskingKey = new byte[4];
            new java.security.SecureRandom().nextBytes(maskingKey);
            continuationFrame.setMaskingKey(maskingKey);
        } else {
            continuationFrame.setMasked(false);
            continuationFrame.setMaskingKey(new byte[0]);
        }

        return continuationFrame;
    }

    /**
     * Delegate standard frame creation to existing factory
     */
    public WebSocketFrame createTextFrame(String text) {
        return delegateFactory.createTextFrame(text);
    }

    public WebSocketFrame createBinaryFrame(byte[] data) {
        return delegateFactory.createBinaryFrame(data);
    }

    public WebSocketFrame createPingFrame() {
        return delegateFactory.createPingFrame();
    }

    public WebSocketFrame createPongFrame() {
        return delegateFactory.createPongFrame();
    }

    public WebSocketFrame createCloseFrame(int code, String reason) {
        return delegateFactory.createCloseFrame(code, reason);
    }

    /**
     * Check if message handler is currently receiving fragments
     */
    public boolean isReceivingFragments() {
        return messageHandler.isReceivingFragments();
    }

    /**
     * Reset fragmentation state
     */
    public void resetFragmentationState() {
        messageHandler.reset();
    }

    /**
     * Get statistics about fragmentation
     */
    public boolean needsFragmentation(String text) {
        return fragmenter.needsFragmentation(text.getBytes());
    }

    public boolean needsFragmentation(byte[] data) {
        return fragmenter.needsFragmentation(data);
    }

    public int calculateFragmentCount(String text) {
        return fragmenter.calculateFragmentCount(text.getBytes());
    }

    public int calculateFragmentCount(byte[] data) {
        return fragmenter.calculateFragmentCount(data);
    }

    private boolean isClientFactory() {
        return delegateFactory.getClass().getSimpleName().contains("Client");
    }
}