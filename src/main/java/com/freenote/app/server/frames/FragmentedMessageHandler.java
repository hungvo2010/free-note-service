package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles fragmented WebSocket messages by reusing existing DataFrame instances
 */
@Getter
@Setter
public class FragmentedMessageHandler {
    private final List<DataFrame> fragments = new ArrayList<>();
    private FrameType originalMessageType = null;
    private boolean isReceivingFragments = false;

    /**
     * Process a frame and determine if it's part of a fragmented message
     *
     * @param frame The frame to process
     * @return Complete message if fragmentation is complete, null if still collecting fragments
     */
    public DataFrame processFrame(WebSocketFrame frame) {
        if (!(frame instanceof DataFrame dataFrame)) {
            throw new IllegalArgumentException("Only DataFrame can be fragmented");
        }

        // Determine frame type from opcode
        FrameType frameType = FrameType.fromHexValue(dataFrame.getOpcode());

        if (!isReceivingFragments) {
            // Not currently receiving fragments
            if (dataFrame.isFin()) {
                // Single complete frame
                return dataFrame;
            } else {
                // Start of new fragmented message
                startFragmentation(frameType, dataFrame);
                return null;
            }
        } else {
            if (frameType != FrameType.CONTINUATION) {
                throw new IllegalStateException("Expected continuation frame but got: " + frameType);
            }
            fragments.add(dataFrame);
            if (dataFrame.isFin()) {
                // Last fragment received, assemble complete message
                return assembleCompleteMessage();
            }
            return null;
        }
    }


    /**
     * Start collecting fragments for a new message
     */
    private void startFragmentation(FrameType messageType, DataFrame firstFrame) {
        this.originalMessageType = messageType;
        this.isReceivingFragments = true;
        this.fragments.clear();
        this.fragments.add(firstFrame);
    }

    /**
     * Assemble all fragments into a single DataFrame reusing existing structure
     */
    private DataFrame assembleCompleteMessage() {
        if (fragments.isEmpty()) {
            throw new IllegalStateException("No fragments to assemble");
        }

        // Calculate total payload size
        int totalPayloadSize = fragments.stream()
                .mapToInt(frame -> frame.getPayloadData().length)
                .sum();

        // Create a combined payload
        byte[] combinedPayload = new byte[totalPayloadSize];
        int offset = 0;

        for (DataFrame fragment : fragments) {
            byte[] fragmentPayload = fragment.getPayloadData();
            System.arraycopy(fragmentPayload, 0, combinedPayload, offset, fragmentPayload.length);
            offset += fragmentPayload.length;
        }

        // Use the first fragment's masking properties for the complete message
        DataFrame firstFragment = fragments.get(0);

        // Create final complete DataFrame reusing existing constructor
        DataFrame completeMessage = new DataFrame(
                originalMessageType.getOpCode(),
                combinedPayload,
                firstFragment.isMasked(),
                firstFragment.getMaskingKey()
        );

        // Mark as final frame
        completeMessage.setFin(true);

        // Reset state for next message
        reset();

        return completeMessage;
    }

    /**
     * Reset handler state
     */
    public void reset() {
        fragments.clear();
        originalMessageType = null;
        isReceivingFragments = false;
    }
}