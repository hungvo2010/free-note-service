package com.freedraw.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ServerResponse {
    private int status;
    private String message;
}
