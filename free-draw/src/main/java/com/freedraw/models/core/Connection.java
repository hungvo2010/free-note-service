<<<<<<<< HEAD:free-draw/src/main/java/com/freedraw/models/core/Connection.java
package com.freedraw.models.core;
========
package com.freedraw.connections;
>>>>>>>> 25da83b6c9f9182369f7ea2b7f7c01d4d535bd32:free-draw/src/main/java/com/freedraw/connections/Connection.java

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Closeable;
import java.io.OutputStream;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class Connection implements Closeable {
    private OutputStream outputStream;
    private boolean open = true;

    public Connection(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void close() {
        this.open = false;
    }
}
