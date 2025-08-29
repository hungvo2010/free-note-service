package com.freenote.app.server.frames;

import com.freenote.app.server.factory.FrameFactory;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates fragmented messages using existing frame classes
 */
public class MessageFragmenter {
    private final FrameFactory frameFactory;
    private final int maxFragmentSize;
    
    public MessageFragmenter(FrameFactory frameFactory, int maxFragmentSize) {
        this.frameFactory = frameFactory;
        this.maxFragmentSize = maxFragmentSize;
    }
    
    /**
     * Fragment a text message into multiple DataFrames
     */
    public List<DataFrame> fragmentTextMessage(String message) {
        return fragmentMessage(FrameType.TEXT, message.getBytes());
    }
    
    /**
     * Fragment a binary message into multiple DataFrames
     */
    public List<DataFrame> fragmentBinaryMessage(byte[] data) {
        return fragmentMessage(FrameType.BINARY, data);
    }
    
    /**
     * Fragment a message of any type into DataFrames
     */
    private List<DataFrame> fragmentMessage(FrameType messageType, byte[] data) {
        List<DataFrame> fragments = new ArrayList<>();
        
        if (data.length <= maxFragmentSize) {
            // No fragmentation needed - create single frame
            WebSocketFrame singleFrame = createSingleFrame(messageType, data);
            fragments.add((DataFrame) singleFrame);
            return fragments;
        }
        
        // Fragmentation needed
        int offset = 0;
        boolean isFirstFragment = true;
        
        while (offset < data.length) {
            int fragmentSize = Math.min(maxFragmentSize, data.length - offset);
            byte[] fragmentData = Arrays.copyOfRange(data, offset, offset + fragmentSize);
            boolean isLastFragment = (offset + fragmentSize >= data.length);
            
            DataFrame fragment;
            
            if (isFirstFragment) {
                // The first fragment uses an original message type
                fragment = createFragment(messageType, fragmentData, false); // not final
                isFirstFragment = false;
            } else {
                // Subsequent fragments use CONTINUATION type
                fragment = createFragment(FrameType.CONTINUATION, fragmentData, isLastFragment);
            }
            
            fragments.add(fragment);
            offset += fragmentSize;
        }
        
        return fragments;
    }
    
    /**
     * Create a single complete frame (not fragmented)
     */
    private WebSocketFrame createSingleFrame(FrameType messageType, byte[] data) {
        return switch (messageType) {
            case TEXT -> frameFactory.createTextFrame(new String(data));
            case BINARY -> frameFactory.createBinaryFrame(data);
            case CONTINUATION -> frameFactory.createContinuationFrame(data);
            default -> throw new IllegalArgumentException("Unsupported message type for fragmentation: " + messageType);
        };
    }
    
    /**
     * Create a fragment using existing DataFrame constructor
     */
    private DataFrame createFragment(FrameType frameType, byte[] data, boolean isFinal) {
        // Reuse DataFrame constructor to create fragment
        DataFrame fragment = new DataFrame(frameType.getOpCode(), data);
        fragment.setFin(isFinal);
        
        // Set masking based on factory type (client vs server)
        if (isClientFactory()) {
            fragment.setMasked(true);
            // Generate masking key for client frames
            byte[] maskingKey = new byte[4];
            new java.security.SecureRandom().nextBytes(maskingKey);
            fragment.setMaskingKey(maskingKey);
        } else {
            fragment.setMasked(false);
            fragment.setMaskingKey(new byte[0]);
        }
        
        return fragment;
    }
    
    /**
     * Detect if this is a client factory based on class name
     */
    private boolean isClientFactory() {
        return frameFactory.getClass().getSimpleName().contains("Client");
    }
    
    /**
     * Calculate total fragments needed for a message
     */
    public int calculateFragmentCount(byte[] data) {
        if (data.length <= maxFragmentSize) {
            return 1;
        }
        return (int) Math.ceil((double) data.length / maxFragmentSize);
    }
    
    /**
     * Check if message needs fragmentation
     */
    public boolean needsFragmentation(byte[] data) {
        return data.length > maxFragmentSize;
    }
}