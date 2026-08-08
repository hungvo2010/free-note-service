package com.freenote.app.io;

import com.freenote.app.server.io.NoHeaderObjectOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoHeaderObjectOutputStreamTest {
    @Test
    void writeSerializableObject_ThenShouldDeserializeCorrectly() throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var noHeaderObjectOutputStream = new NoHeaderObjectOutputStream(byteArrayOutputStream);

        String original = "Test String";
        noHeaderObjectOutputStream.writeObject(original);
        noHeaderObjectOutputStream.flush();

        assertEquals(0, byteArrayOutputStream.size());
    }
}
