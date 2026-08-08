package com.freenote.app.handler;

import org.junit.jupiter.api.Test;

import java.net.Socket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketHandlerTest {
    @Test
    void givenClosedWebSocket_whenCallHandle_ThenMustRunWithoutException() {

        var socket = mock(Socket.class);
        
        when(socket.isClosed()).thenReturn(true);


    }
}
