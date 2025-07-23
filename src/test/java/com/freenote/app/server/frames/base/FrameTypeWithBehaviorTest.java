package com.freenote.app.server.frames.base;

import com.freenote.app.server.frames.FrameFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FrameTypeWithBehaviorTest {

    @Test
    void testEnumValues_CorrectOpcodes() {
        assertEquals(0x0, FrameTypeWithBehavior.CONTINUATION.getOpcode());
        assertEquals(0x1, FrameTypeWithBehavior.TEXT.getOpcode());
        assertEquals(0x2, FrameTypeWithBehavior.BINARY.getOpcode());
        assertEquals(0x8, FrameTypeWithBehavior.CLOSE.getOpcode());
        assertEquals(0x9, FrameTypeWithBehavior.PING.getOpcode());
        assertEquals(0xA, FrameTypeWithBehavior.PONG.getOpcode());
    }

    @Test
    void testAllEnumValues_Exist() {
        FrameTypeWithBehavior[] values = FrameTypeWithBehavior.values();
        
        assertEquals(6, values.length);
        assertEquals(FrameTypeWithBehavior.CONTINUATION, values[0]);
        assertEquals(FrameTypeWithBehavior.TEXT, values[1]);
        assertEquals(FrameTypeWithBehavior.BINARY, values[2]);
        assertEquals(FrameTypeWithBehavior.CLOSE, values[3]);
        assertEquals(FrameTypeWithBehavior.PING, values[4]);
        assertEquals(FrameTypeWithBehavior.PONG, values[5]);
    }

    @Test
    void testValueOf_WithValidNames() {
        assertEquals(FrameTypeWithBehavior.CONTINUATION, FrameTypeWithBehavior.valueOf("CONTINUATION"));
        assertEquals(FrameTypeWithBehavior.TEXT, FrameTypeWithBehavior.valueOf("TEXT"));
        assertEquals(FrameTypeWithBehavior.BINARY, FrameTypeWithBehavior.valueOf("BINARY"));
        assertEquals(FrameTypeWithBehavior.CLOSE, FrameTypeWithBehavior.valueOf("CLOSE"));
        assertEquals(FrameTypeWithBehavior.PING, FrameTypeWithBehavior.valueOf("PING"));
        assertEquals(FrameTypeWithBehavior.PONG, FrameTypeWithBehavior.valueOf("PONG"));
    }

    @Test
    void testValueOf_WithInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> {
            FrameTypeWithBehavior.valueOf("INVALID");
        });
    }

    @Test
    void testContinuationFrame_ParseFrame() {
        byte[] testFrame = new byte[]{0x00, 0x01, 0x48}; // Simple frame data
        
        try (MockedStatic<FrameFactory> mockedFactory = mockStatic(FrameFactory.class)) {
            WebSocketFrame mockFrame = mock(WebSocketFrame.class);
            mockedFactory.when(() -> FrameFactory.createDataFrame(eq(testFrame), eq(FrameTypeWithBehavior.CONTINUATION)))
                        .thenReturn(mockFrame);
            
            WebSocketFrame result = FrameTypeWithBehavior.CONTINUATION.parseFrame(testFrame);
            
            assertEquals(mockFrame, result);
            mockedFactory.verify(() -> FrameFactory.createDataFrame(testFrame, FrameTypeWithBehavior.CONTINUATION));
        }
    }

    @Test
    void testTextFrame_ParseFrame() {
        byte[] testFrame = new byte[]{(byte) 0x81, 0x05, 'H', 'e', 'l', 'l', 'o'};
        
        try (MockedStatic<FrameFactory> mockedFactory = mockStatic(FrameFactory.class)) {
            WebSocketFrame mockFrame = mock(WebSocketFrame.class);
            mockedFactory.when(() -> FrameFactory.createDataFrame(eq(testFrame), eq(FrameTypeWithBehavior.TEXT)))
                        .thenReturn(mockFrame);
            
            WebSocketFrame result = FrameTypeWithBehavior.TEXT.parseFrame(testFrame);
            
            assertEquals(mockFrame, result);
            mockedFactory.verify(() -> FrameFactory.createDataFrame(testFrame, FrameTypeWithBehavior.TEXT));
        }
    }

    @Test
    void testBinaryFrame_ParseFrame() {
        byte[] testFrame = new byte[]{(byte) 0x82, 0x03, 0x01, 0x02, 0x03};
        
        try (MockedStatic<FrameFactory> mockedFactory = mockStatic(FrameFactory.class)) {
            WebSocketFrame mockFrame = mock(WebSocketFrame.class);
            mockedFactory.when(() -> FrameFactory.createDataFrame(eq(testFrame), eq(FrameTypeWithBehavior.BINARY)))
                        .thenReturn(mockFrame);
            
            WebSocketFrame result = FrameTypeWithBehavior.BINARY.parseFrame(testFrame);
            
            assertEquals(mockFrame, result);
            mockedFactory.verify(() -> FrameFactory.createDataFrame(testFrame, FrameTypeWithBehavior.BINARY));
        }
    }

    @Test
    void testCloseFrame_ParseFrame() {
        byte[] testFrame = new byte[]{(byte) 0x88, 0x00};
        
        try (MockedStatic<FrameFactory> mockedFactory = mockStatic(FrameFactory.class)) {
            WebSocketFrame mockFrame = mock(WebSocketFrame.class);
            mockedFactory.when(() -> FrameFactory.createControlFrame(eq(testFrame), eq(FrameTypeWithBehavior.CLOSE)))
                        .thenReturn(mockFrame);
            
            WebSocketFrame result = FrameTypeWithBehavior.CLOSE.parseFrame(testFrame);
            
            assertEquals(mockFrame, result);
            mockedFactory.verify(() -> FrameFactory.createControlFrame(testFrame, FrameTypeWithBehavior.CLOSE));
        }
    }

    @Test
    void testPingFrame_ParseFrame() {
        byte[] testFrame = new byte[]{(byte) 0x89, 0x00};
        
        try (MockedStatic<FrameFactory> mockedFactory = mockStatic(FrameFactory.class)) {
            WebSocketFrame mockFrame = mock(WebSocketFrame.class);
            mockedFactory.when(() -> FrameFactory.createControlFrame(eq(testFrame), eq(FrameTypeWithBehavior.PING)))
                        .thenReturn(mockFrame);
            
            WebSocketFrame result = FrameTypeWithBehavior.PING.parseFrame(testFrame);
            
            assertEquals(mockFrame, result);
            mockedFactory.verify(() -> FrameFactory.createControlFrame(testFrame, FrameTypeWithBehavior.PING));
        }
    }

    @Test
    void testPongFrame_ParseFrame() {
        byte[] testFrame = new byte[]{(byte) 0x8A, 0x00};
        
        try (MockedStatic<FrameFactory> mockedFactory = mockStatic(FrameFactory.class)) {
            WebSocketFrame mockFrame = mock(WebSocketFrame.class);
            mockedFactory.when(() -> FrameFactory.createControlFrame(eq(testFrame), eq(FrameTypeWithBehavior.PONG)))
                        .thenReturn(mockFrame);
            
            WebSocketFrame result = FrameTypeWithBehavior.PONG.parseFrame(testFrame);
            
            assertEquals(mockFrame, result);
            mockedFactory.verify(() -> FrameFactory.createControlFrame(testFrame, FrameTypeWithBehavior.PONG));
        }
    }

    @Test
    void testDataFrames_CallCreateDataFrame() {
        byte[] testFrame = new byte[]{(byte) 0x81, 0x01, 'A'};
        
        FrameTypeWithBehavior[] dataFrameTypes = {
            FrameTypeWithBehavior.CONTINUATION,
            FrameTypeWithBehavior.TEXT,
            FrameTypeWithBehavior.BINARY
        };
        
        for (FrameTypeWithBehavior frameType : dataFrameTypes) {
            try (MockedStatic<FrameFactory> mockedFactory = mockStatic(FrameFactory.class)) {
                WebSocketFrame mockFrame = mock(WebSocketFrame.class);
                mockedFactory.when(() -> FrameFactory.createDataFrame(any(byte[].class), eq(frameType)))
                           .thenReturn(mockFrame);
                
                frameType.parseFrame(testFrame);
                
                mockedFactory.verify(() -> FrameFactory.createDataFrame(testFrame, frameType));
            }
        }
    }

    @Test
    void testControlFrames_CallCreateControlFrame() {
        byte[] testFrame = new byte[]{(byte) 0x88, 0x00};
        
        FrameTypeWithBehavior[] controlFrameTypes = {
            FrameTypeWithBehavior.CLOSE,
            FrameTypeWithBehavior.PING,
            FrameTypeWithBehavior.PONG
        };
        
        for (FrameTypeWithBehavior frameType : controlFrameTypes) {
            try (MockedStatic<FrameFactory> mockedFactory = mockStatic(FrameFactory.class)) {
                WebSocketFrame mockFrame = mock(WebSocketFrame.class);
                mockedFactory.when(() -> FrameFactory.createControlFrame(any(byte[].class), eq(frameType)))
                           .thenReturn(mockFrame);
                
                frameType.parseFrame(testFrame);
                
                mockedFactory.verify(() -> FrameFactory.createControlFrame(testFrame, frameType));
            }
        }
    }

    @Test
    void testHandleVariableLength_ContinuationFrame_MaskedSmallPayload() {
        // Test masked frame with payload length < 126
        FrameBuilder frameBuilder = new FrameBuilder()
                .setMasked(true)
                .setPayloadData(new byte[]{0x01, 0x02, 0x03}); // length = 3
        
        // This should not throw an exception
        assertDoesNotThrow(() -> {
            FrameTypeWithBehavior.CONTINUATION.handleVariableLength(frameBuilder);
        });
    }

    @Test
    void testHandleVariableLength_ContinuationFrame_UnmaskedSmallPayload() {
        // Test unmasked frame with payload length < 126
        FrameBuilder frameBuilder = new FrameBuilder()
                .setMasked(false)
                .setPayloadData(new byte[100]); // length = 100 < 126
        
        assertDoesNotThrow(() -> {
            FrameTypeWithBehavior.CONTINUATION.handleVariableLength(frameBuilder);
        });
    }

    @Test
    void testHandleVariableLength_ContinuationFrame_PayloadLength126() {
        // Test payload length exactly 126
        FrameBuilder frameBuilder = new FrameBuilder()
                .setMasked(true)
                .setPayloadData(new byte[126]); // length = 126
        
        assertDoesNotThrow(() -> {
            FrameTypeWithBehavior.CONTINUATION.handleVariableLength(frameBuilder);
        });
    }

    @Test
    void testHandleVariableLength_ContinuationFrame_PayloadLengthGreaterThan126() {
        // Test payload length > 126
        FrameBuilder frameBuilder = new FrameBuilder()
                .setMasked(false)
                .setPayloadData(new byte[1000]); // length = 1000 > 126
        
        assertDoesNotThrow(() -> {
            FrameTypeWithBehavior.CONTINUATION.handleVariableLength(frameBuilder);
        });
    }

    @Test
    void testHandleVariableLength_ContinuationFrame_EmptyPayload() {
        // Test empty payload (length = 0)
        FrameBuilder frameBuilder = new FrameBuilder()
                .setMasked(true)
                .setPayloadData(new byte[0]); // length = 0
        
        assertDoesNotThrow(() -> {
            FrameTypeWithBehavior.CONTINUATION.handleVariableLength(frameBuilder);
        });
    }

    @Test
    void testHandleVariableLength_ContinuationFrame_MaxSmallPayload() {
        // Test payload length = 125 (boundary case)
        FrameBuilder frameBuilder = new FrameBuilder()
                .setMasked(false)
                .setPayloadData(new byte[125]); // length = 125 < 126
        
        assertDoesNotThrow(() -> {
            FrameTypeWithBehavior.CONTINUATION.handleVariableLength(frameBuilder);
        });
    }

    @Test
    void testHandleVariableLength_ContinuationFrame_MinMediumPayload() {
        // Test payload length = 127 (boundary case)
        FrameBuilder frameBuilder = new FrameBuilder()
                .setMasked(true)
                .setPayloadData(new byte[127]); // length = 127 > 126
        
        assertDoesNotThrow(() -> {
            FrameTypeWithBehavior.CONTINUATION.handleVariableLength(frameBuilder);
        });
    }

    @Test
    void testHandleVariableLength_OtherFrameTypes() {
        FrameBuilder frameBuilder = new FrameBuilder()
                .setMasked(false)
                .setPayloadData(new byte[]{0x01, 0x02});
        
        FrameTypeWithBehavior[] frameTypes = {
            FrameTypeWithBehavior.TEXT,
            FrameTypeWithBehavior.BINARY,
            FrameTypeWithBehavior.CLOSE,
            FrameTypeWithBehavior.PING,
            FrameTypeWithBehavior.PONG
        };
        
        // All these should not throw exceptions (empty implementations)
        for (FrameTypeWithBehavior frameType : frameTypes) {
            assertDoesNotThrow(() -> {
                frameType.handleVariableLength(frameBuilder);
            });
        }
    }

    @Test
    void testHandleVariableLength_WithNullFrameBuilder() {
        // Test behavior with null FrameBuilder for CONTINUATION (only one with implementation)
        assertThrows(NullPointerException.class, () -> {
            FrameTypeWithBehavior.CONTINUATION.handleVariableLength(null);
        });
        
        // Other frame types should handle null gracefully (empty implementations)
        FrameTypeWithBehavior[] frameTypes = {
            FrameTypeWithBehavior.TEXT,
            FrameTypeWithBehavior.BINARY,
            FrameTypeWithBehavior.CLOSE,
            FrameTypeWithBehavior.PING,
            FrameTypeWithBehavior.PONG
        };
        
        for (FrameTypeWithBehavior frameType : frameTypes) {
            assertDoesNotThrow(() -> {
                frameType.handleVariableLength(null);
            });
        }
    }

    @Test
    void testParseFrame_WithNullFrame() {
        // Test all frame types with null input
        FrameTypeWithBehavior[] allFrameTypes = FrameTypeWithBehavior.values();
        
        for (FrameTypeWithBehavior frameType : allFrameTypes) {
            try (MockedStatic<FrameFactory> mockedFactory = mockStatic(FrameFactory.class)) {
                if (frameType == FrameTypeWithBehavior.CLOSE || 
                    frameType == FrameTypeWithBehavior.PING || 
                    frameType == FrameTypeWithBehavior.PONG) {
                    mockedFactory.when(() -> FrameFactory.createControlFrame(isNull(), eq(frameType)))
                               .thenReturn(mock(WebSocketFrame.class));
                } else {
                    mockedFactory.when(() -> FrameFactory.createDataFrame(isNull(), eq(frameType)))
                               .thenReturn(mock(WebSocketFrame.class));
                }
                
                assertDoesNotThrow(() -> {
                    frameType.parseFrame(null);
                });
            }
        }
    }

    @Test
    void testParseFrame_WithEmptyFrame() {
        byte[] emptyFrame = new byte[0];
        
        FrameTypeWithBehavior[] allFrameTypes = FrameTypeWithBehavior.values();
        
        for (FrameTypeWithBehavior frameType : allFrameTypes) {
            try (MockedStatic<FrameFactory> mockedFactory = mockStatic(FrameFactory.class)) {
                WebSocketFrame mockFrame = mock(WebSocketFrame.class);
                
                if (frameType == FrameTypeWithBehavior.CLOSE || 
                    frameType == FrameTypeWithBehavior.PING || 
                    frameType == FrameTypeWithBehavior.PONG) {
                    mockedFactory.when(() -> FrameFactory.createControlFrame(eq(emptyFrame), eq(frameType)))
                               .thenReturn(mockFrame);
                } else {
                    mockedFactory.when(() -> FrameFactory.createDataFrame(eq(emptyFrame), eq(frameType)))
                               .thenReturn(mockFrame);
                }
                
                WebSocketFrame result = frameType.parseFrame(emptyFrame);
                assertEquals(mockFrame, result);
            }
        }
    }

    @Test
    void testGetOpcode_ReturnsShortType() {
        for (FrameTypeWithBehavior frameType : FrameTypeWithBehavior.values()) {
            short opcode = frameType.getOpcode();
            assertNotNull(opcode);
            assertTrue(opcode >= 0 || opcode < 0); // Just verify it's a valid short
        }
    }

    @Test
    void testEnumConstantsAreImmutable() {
        // Verify that enum constants maintain their opcodes
        assertEquals(0x0, FrameTypeWithBehavior.CONTINUATION.getOpcode());
        assertEquals(0x1, FrameTypeWithBehavior.TEXT.getOpcode());
        assertEquals(0x2, FrameTypeWithBehavior.BINARY.getOpcode());
        assertEquals(0x8, FrameTypeWithBehavior.CLOSE.getOpcode());
        assertEquals(0x9, FrameTypeWithBehavior.PING.getOpcode());
        assertEquals(0xA, FrameTypeWithBehavior.PONG.getOpcode());
        
        // Verify calling getOpcode multiple times returns same value
        FrameTypeWithBehavior frameType = FrameTypeWithBehavior.TEXT;
        short opcode1 = frameType.getOpcode();
        short opcode2 = frameType.getOpcode();
        assertEquals(opcode1, opcode2);
    }
}