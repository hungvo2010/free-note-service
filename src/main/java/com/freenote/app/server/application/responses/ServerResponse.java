package com.freenote.app.server.application.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ServerResponse {
    private int status;
    private String message;
}
