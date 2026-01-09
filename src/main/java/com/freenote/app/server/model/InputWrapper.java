package com.freenote.app.server.model;

import com.freenote.app.server.data.ws.RequestObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.net.Socket;

@Getter
@Setter
@AllArgsConstructor
public class InputWrapper {
    private InputStream inputStream;
    private Socket socket;
    private RequestObject requestObject;

    public InputWrapper(InputStream input, Socket incomingSocket) {
        this.inputStream = input;
        this.socket = incomingSocket;
    }

    public InputWrapper(InputStream input) {
        this.inputStream = input;
    }
}
