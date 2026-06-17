package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.context.ReadableContext;
import com.freenote.app.server.core.nio.ModernIncomingConnectionHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Stack;

public interface ConnectionState {
    void handle(ModernIncomingConnectionHandler handler, ReadableContext context) throws IOException;

    ByteBuffer getByteBuffer();

    default public String decodeString(String s) {
        Stack<Object> arr = new Stack<Object>();
        int tempNumber = 0;
        String tempStr = "";
        for (int i = 0; i < s.length(); ++i){
            char c = s.charAt(i);
            if (c >= '0' && c <= '9'){
                tempNumber = tempNumber * 10 + (c - '0');
            }
            else if (c == '['){
                if (tempNumber != 0){
                    arr.push(tempNumber);
                    tempNumber = 0;
                }
            }
            else if (c == ']'){
                handleCloseBracket(arr, tempStr);
            }
            // character
            else {
                tempStr = tempStr + c;
            }

        }
        return tempStr;
    }

    default void handleCloseBracket(Stack<Object> arr, String tempStr){

    }
}
