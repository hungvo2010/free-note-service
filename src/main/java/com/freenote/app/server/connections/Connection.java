package com.freenote.app.server.connections;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.OutputStream;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class Connection {
    private OutputStream outputStream;
}
