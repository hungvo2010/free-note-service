package com.freenote.app.frame;

import com.freenote.app.server.factory.ClientFrameFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientFrameTest {
    static ClientFrameFactory clientFrameFactory = null;

    @BeforeAll
    static void setup() {
        clientFrameFactory = new ClientFrameFactory();
    }

    @Test
    void testClientFrameCreation() {
        var clientFrame = clientFrameFactory.createPongFrame();
        assertTrue(clientFrame.isMasked());
    }
}
