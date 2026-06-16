# Model Classes Dump

## File: src/main/java/com/freenote/app/server/model/enums/MsgType.java
```java
package com.freenote.app.server.model.enums;

public enum MsgType 
    PING,
    PONG

```

## File: src/main/java/com/freenote/app/server/model/InputWrapper.java
```java
package com.freenote.app.server.model;

import com.freenote.app.server.model.ws.AppRequestData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InputWrapper 
    private Socket socket;
    private ByteBuffer channelBuffer;
    private SocketChannel socketChannel;
    private CommonRequestObject requestObject;
    private InputStream inputStream;

    public InputWrapperSocket incomingSocket 
        this.socket = incomingSocket;
    

    public InputWrapper 
    

    public InputStream getInputStream 
        if inputStream != null 
            return inputStream;
        
        try 
            if requestObject != null && requestObject.getSocket != null 
                return requestObject.getSocket.getInputStream;
            
            if socket != null 
                return socket.getInputStream;
            
         catch IOException e 
            throw new RuntimeExceptione;
        
        return null;
    

```

## File: src/main/java/com/freenote/app/server/model/http/HttpUpgradeResponse.java
```java
package com.freenote.app.server.model.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class HttpUpgradeResponse 
    //    HTTP/1.1 101 Switching Protocols
//    Upgrade: websocket
//    Connection: Upgrade
//    Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
    private String version;
    @Builder.Default
    private String statusCode = "-1";
    private String statusText;
    private String upgrade;
    private String connection;
    private String secWebSocketAccept;
    private HttpUpgradeRequest httpUpgradeRequest;
    public static final HttpUpgradeResponse EMPTY_UPGRADE_RESPONSE = new HttpUpgradeResponse;

    @Override
    public String toString 
        String statusLine = String.join" ", version, statusCode, statusText;
        return String.join"\r\n",
                statusLine,
                "Upgrade: " + upgrade,
                "Connection: " + connection,
                "Sec-WebSocket-Accept: " + secWebSocketAccept,
                "", "" // required to terminate HTTP headers
        ;
    

    public byte toRawBytes 
        return this.toString.getBytesStandardCharsets.UTF_8;
    

```

## File: src/main/java/com/freenote/app/server/model/http/HttpUpgradeRequest.java
```java
package com.freenote.app.server.model.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HttpUpgradeRequest 
    //    GET /chat HTTP/1.1
//    Host: server.example.com
//    Upgrade: websocket
//    Connection: Upgrade
//    Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
//    Origin: http://example.com
//    Sec-WebSocket-Protocol: chat, superchat
//    Sec-WebSocket-Version: 13
    private String method;
    private String uri;
    private String path;
    private String version;
    private String host;
    private String upgrade;
    private String connection;
    private String secWebSocketKey;
    private String origin;
    private String secWebSocketVersion;
    private String secWebSocketExtensions;
    private String webSocketProtocol;
    public static final HttpUpgradeRequest EMPTY_UPGRADE_REQUEST = new HttpUpgradeRequest;

    public static class HttpUpgradeRequestBuilder 
        public HttpUpgradeRequestBuilder uriString uri 
            this.uri = uri;
            this.path = URI.createuri.getPath;
            return this;
        

        public HttpUpgradeRequest build 
            if secWebSocketKey == null || secWebSocketKey.isEmpty
                    || secWebSocketVersion == null || secWebSocketVersion.isEmpty 
                return EMPTY_UPGRADE_REQUEST;
            
            return new HttpUpgradeRequest
                    this.method,
                    this.uri,
                    this.path,
                    this.version,
                    this.host,
                    this.upgrade,
                    this.connection,
                    this.secWebSocketKey,
                    this.origin,
                    this.secWebSocketVersion,
                    this.secWebSocketExtensions,
                    this.webSocketProtocol
            ;
        
    

    @Override
    public String toString 
        return String.join"\r\n",
                method + " " + path + " " + version,
                "Host: " + host,
                "Upgrade: " + upgrade,
                "Connection: " + connection,
                "Sec-WebSocket-Key: " + secWebSocketKey,
                "Origin: " + origin,
                "Sec-WebSocket-Version: " + secWebSocketVersion,
                "Sec-WebSocket-Extensions: " + secWebSocketExtensions,
                "Sec-WebSocket-Protocol: " + webSocketProtocol,
                "", "" // required to terminate HTTP headers
        ;
    

```

## File: src/main/java/com/freenote/app/server/model/TraceResponseData.java
```java
package com.freenote.app.server.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TraceResponseData 
    private String requestId;
    private String traceId;
    private long timestamp;

    public TraceResponseData 
        requestId = UUID.randomUUID.toString;
        traceId = UUID.randomUUID.toString;
        timestamp = System.currentTimeMillis;
    

```

## File: src/main/java/com/freenote/app/server/model/OutputWrapper.java
```java
package com.freenote.app.server.model;

import java.io.OutputStream;


public record OutputWrapperOutputStream outputStream 


```

## File: src/main/java/com/freenote/app/server/model/TraceRequestData.java
```java
package com.freenote.app.server.model;

import lombok.Data;

import java.util.UUID;

@Data
public class TraceRequestData 
    private String requestId;
    private String traceId;
    private long timestamp;

    public TraceRequestData 
        requestId = UUID.randomUUID.toString;
        traceId = UUID.randomUUID.toString;
        timestamp = System.currentTimeMillis;
    

```

## File: src/main/java/com/freenote/app/server/model/ws/CommonResponseObject.java
```java
package com.freenote.app.server.model.ws;

import com.freenote.app.server.model.TraceResponseData;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommonResponseObject<T extends TraceResponseData> 
    private T responseData;

    public T getResponseDataClass<T> clazz 
        return clazz.castresponseData;
    

```

## File: src/main/java/com/freenote/app/server/model/ws/CommonRequestObject.java
```java
package com.freenote.app.server.model.ws;

import com.freenote.app.server.model.TraceRequestData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.net.Socket;

@AllArgsConstructor
@Builder
@Setter
@Getter
public class CommonRequestObject<T extends TraceRequestData> 
    private Socket socket;
    private String origin;
    private T requestData;

    private T getRequestDataClass<T> clazz 
        return clazz.castrequestData;
    

```
