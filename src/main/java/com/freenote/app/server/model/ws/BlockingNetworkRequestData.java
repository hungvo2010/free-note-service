package com.freenote.app.server.model.ws;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.parser.FullFrameParser;
import com.freenote.app.server.util.IOUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class BlockingNetworkRequestData implements NetworkRequestData {
    private final Socket socket;
    /**
     * Lấy MỘT lần rồi dùng lại cho mọi lần đọc.
     * Với SSLSocket, sau khi peer gửi close_notify thì getInputStream() NÉM
     * SocketException("Socket input is already shutdown") chứ không trả stream,
     * nên gọi lại nó trong vòng lặp là nguồn của log storm.
     */
    private final InputStream inputStream;
    private volatile boolean readClosed;

    public BlockingNetworkRequestData(Socket socket) {
        this.socket = socket;
        this.inputStream = openInputStream(socket);
    }

    private static InputStream openInputStream(Socket socket) {
        try {
            return socket.getInputStream();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to obtain input stream from socket", e);
        }
    }

    /**
     * Đánh dấu phía client đã đóng / EOF để vòng đọc phía trên dừng lại
     * (socket.isClosed() không đủ tin cậy với SSLSocket sau close_notify).
     */
    public void markReadClosed() {
        this.readClosed = true;
    }

    public boolean isReadClosed() {
        return readClosed;
    }

    @Override
    public WebSocketFrame buildRequestFrame() {
        return null;
    }

    @Override
    public WebSocketFrame buildResponseFrame() {
        return null;
    }

    @Override
    public byte[] readFrameBytes() throws IOException {
        if (readClosed) {
            throw new EOFException("Connection already closed by peer");
        }
        try {
            return new FullFrameParser().getRawBytes(inputStream);
        } catch (SocketException e) {
            // SSLSocket ném exception này khi peer đã close_notify thay vì trả -1
            markReadClosed();
            throw new EOFException("Client closed the connection: " + e.getMessage());
        }
    }

    @Override
    public int read(byte[] data) throws IOException {
        if (readClosed) {
            return -1;
        }
        try {
            int read = inputStream.read(data);
            if (read == -1) {
                markReadClosed();
            }
            return read;
        } catch (SocketException e) {
            markReadClosed();
            return -1;
        }
    }

    @Override
    public void write(byte[] data) throws IOException {
        IOUtils.writeOutPut(socket.getOutputStream(), data);
    }

    @Override
    public byte[] read() throws IOException {
        var bytes = new byte[8192];
        int count = this.read(bytes);
        if (count == -1) {
            return new byte[0];
        }
        return java.util.Arrays.copyOf(bytes, count);
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public boolean isClosed() {
        return readClosed || socket == null || socket.isClosed();
    }

    @Override
    public Object getRemoteAddress() {
        if (socket != null) {
            return socket.getRemoteSocketAddress();
        }
        return null;
    }

    @Override
    public void prepareForRead() {

    }

    // TODO: break encapsulation
    public OutputStream getOutputStream() {
        try {
            return socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get output stream from socket", e);
        }
    }
}
