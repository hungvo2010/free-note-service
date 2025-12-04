package com.freenote.app.handler;

import com.freenote.app.server.connections.IncomingSocketHandlerImpl;
import org.junit.jupiter.api.Test;

import java.net.Socket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebSocketHandlerTest {
    @Test
    void givenClosedWebSocket_whenCallHandle_ThenMustRunWithoutException() {
        var handler = new IncomingSocketHandlerImpl();
        var socket = mock(Socket.class);
        
        when(socket.isClosed()).thenReturn(true);


    }
}
