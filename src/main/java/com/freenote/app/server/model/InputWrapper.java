package com.freenote.app.server.model;

import com.freenote.app.server.model.ws.CommonRequestObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

@Getter
@Setter
@AllArgsConstructor
public class InputWrapper {
    private Socket socket;
    private CommonRequestObject requestObject;

    public InputWrapper(Socket incomingSocket) {
        this.socket = incomingSocket;
    }

    public InputWrapper() {
    }

    public InputStream getInputStream() {
        try {
            return requestObject.getSocket().getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
