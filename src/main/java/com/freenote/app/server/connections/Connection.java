package com.freenote.app.server.connections;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.OutputStream;

@AllArgsConstructor
@Getter
public class Connection {
    private OutputStream outputStream;
}
