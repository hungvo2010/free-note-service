package com.freenote.app.server.connections;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.Socket;

@AllArgsConstructor
@Getter
public class Connection {
    private Socket socket;
    private String path;
    private String sourceIp;
}
