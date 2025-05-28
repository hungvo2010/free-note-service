package com.freenote.app.server.frames;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.*;
import java.util.BitSet;

@AllArgsConstructor
@Builder
@NoArgsConstructor
public class BaseFrame implements Serializable, Externalizable {
    @Serial
    private static final long serialVersionUID = -2140098214102580912L;
    private boolean fin;
    private boolean rsv1;
    private boolean rsv2;
    private boolean rsv3;
    private BitSet opcode;
    private boolean mask;
    private long payloadLength;
    private byte[] maskingKey;
    private byte[] payloadData;
    private byte[] extensionData;
    private byte[] applicationData;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeBoolean(fin);
        out.writeBoolean(rsv1);
        out.writeBoolean(rsv2);
        out.writeBoolean(rsv3);
        out.writeObject(opcode);
        out.writeBoolean(mask);
        out.writeLong(payloadLength);
        out.writeObject(maskingKey);
        out.writeObject(payloadData);
        out.writeObject(extensionData);
        out.writeObject(applicationData);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
       fin = in.readBoolean();
    }
}
