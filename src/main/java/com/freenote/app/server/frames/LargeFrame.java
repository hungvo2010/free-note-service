package com.freenote.app.server.frames;

import com.freenote.app.server.exceptions.InvalidFrameStateException;
import com.freenote.app.server.factory.ServerFrameFactory;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.util.FrameUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class LargeFrame {
    private FragmentState fragmentState;
    private final List<DataFrame> fragmentMessages;
    private WebSocketFrame mergedFrame;
    private final ServerFrameFactory serverFrameFactory;

    public LargeFrame() {
        serverFrameFactory = new ServerFrameFactory();
        fragmentMessages = new ArrayList<>();
        fragmentState = FragmentState.NO_INIT;
    }

    public void addFragmentMessage(DataFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("Frame cannot be null");
        }
        if (fragmentState == FragmentState.NO_INIT) {
            addFragment(frame);
            updateFragmentState(frame.isFin() ? FragmentState.COMPLETED : FragmentState.FIRST_FRAGMENT);
        } else if (fragmentState == FragmentState.FIRST_FRAGMENT || fragmentState == FragmentState.CONTINUATION_FRAGMENT) {
            addFragment(frame);
            updateFragmentState(frame.isFin() ? FragmentState.COMPLETED : FragmentState.CONTINUATION_FRAGMENT);
        } else if (fragmentState == FragmentState.COMPLETED) {
            throw new IllegalStateException("Cannot add more fragments, message is already completed");
        }
    }

    private void updateFragmentState(FragmentState fragmentState) {
        this.fragmentState = fragmentState;
        if (fragmentState == FragmentState.COMPLETED) {
            mergeFragmentsIfCompleted();
        }
    }

    private void mergeFragmentsIfCompleted() {
        if (fragmentState != FragmentState.COMPLETED) {
            throw new InvalidFrameStateException("Cannot merge fragments, message is not completed");
        }
        long totalLength = fragmentMessages.stream().map(DataFrame::getPayloadLength).reduce(0L, Long::sum);
        var mergedPayload = ByteBuffer.allocate((int) totalLength);
        for (var fragment : fragmentMessages) {
            mergedPayload.put(fragment.getPayloadData());
        }
        mergedFrame = new DataFrame(fragmentMessages.get(0).getOpcode(), mergedPayload.array());
    }

    private void addFragment(DataFrame fragment) {
        var rawPayload = FrameUtil.maskPayload(fragment.getPayloadData(), fragment.getMaskingKey());
        this.fragmentMessages.add((DataFrame) serverFrameFactory.createBinaryFrame(rawPayload));
    }

    public WebSocketFrame getMergedFrame() {
        if (fragmentState != FragmentState.COMPLETED) {
            throw new InvalidFrameStateException("Cannot get merged frame, message is not completed");
        }
        return new DataFrame(fragmentMessages.get(0).getOpcode(), mergedFrame.getPayloadData().clone());
    }

    public boolean isComplete() {
        return fragmentState == FragmentState.COMPLETED;
    }
}
