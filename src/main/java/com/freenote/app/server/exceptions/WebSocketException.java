package com.freenote.app.server.exceptions;

public class WebSocketException extends Throwable {
    public WebSocketException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketException(String message) {
        super(message);
    }

    public static class AcceptConnectionException extends RuntimeException {
        public AcceptConnectionException(String s, Exception e) {
            super(e);
        }

        public AcceptConnectionException(String err) {
            super(err);
        }
    }

    public static class ClientDisconnectException extends RuntimeException {
        public ClientDisconnectException(String message, Exception ex) {
            super(message, ex);
        }

        public ClientDisconnectException(String message) {
            super(message);
        }
    }

    public static class ConnectionException extends RuntimeException {
        public ConnectionException(String errorMsg, Throwable cause) {
            super(errorMsg, cause);
        }
    }

    public static class DiskReadException extends RuntimeException {
        public DiskReadException(String description, Throwable cause) {
            super(description, cause);
        }
    }

    public static class HandshakeException extends RuntimeException {
        public HandshakeException(String message) {
            super(message);
        }

        public HandshakeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidFrameException extends RuntimeException {
        public InvalidFrameException(String message) {
            super(message);
        }

        public InvalidFrameException(String message, Exception e) {
            super(message, e);
        }
    }

    public static class InvalidFrameStateException extends RuntimeException {
        public InvalidFrameStateException(String message) {
            super(message);
        }
    }

    public static class MessageParsingException extends RuntimeException {
        public MessageParsingException(String message) {
            super(message);
        }
        public MessageParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MessagePayloadParsingException extends RuntimeException {
        public MessagePayloadParsingException(String description, Throwable cause) {
            super(description, cause);
        }
    }

    public static class NIOReadException extends Throwable {
        public NIOReadException(Exception e) {
            super(e);
        }
    }

    public static class NIOServerInitializationException extends RuntimeException {
        public NIOServerInitializationException(String s, Throwable cause) {
            super(s, cause);
        }
    }

    public static class SelectorInterruptException extends RuntimeException {
        public SelectorInterruptException(String message, Exception cause) {
            super(message, cause);
        }

        public SelectorInterruptException(String message) {
            super(message);
        }
    }

    public static class SocketCreationException extends Exception {
        public SocketCreationException(String message, Throwable cause) {
            super(message, cause);
        }

        public SocketCreationException(String message) {
            super(message);
        }
    }
}
