package com.freenote.app.server.util;

import com.freenote.app.server.frames.base.WebSocketFrame;
import io.NoHeaderObjectOutputStream;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.OutputStream;

@UtilityClass
public class IOUtils {
    public static void writeOutPut(OutputStream outputStream, WebSocketFrame mergedFrame) throws IOException {
        var objectOutputStream = new NoHeaderObjectOutputStream(outputStream);
        objectOutputStream.writeObject(mergedFrame);
        objectOutputStream.flush();
    }
}
