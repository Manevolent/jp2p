package com.manevolent.jp2p.packet;

import com.manevolent.jp2p.packet.io.DataSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Packet {

    /**
     * Reads the packet data from the stream.
     * @param dataInputStream DataInputStream.
     */
    public abstract void read(DataInputStream dataInputStream) throws IOException;

    /**
     * Writes the packet data onto the stream.
     * @param dataOutputStream DataOutputStream.
     */
    public abstract void write(DataOutputStream dataOutputStream) throws IOException;


    public static final byte[] readBytes(DataInputStream dataInputStream) throws IOException {
        short len = dataInputStream.readShort();
        byte[] bytes = new byte[len];
        dataInputStream.read(bytes);

        return bytes;
    }

    public static final void writeBytes(byte[] bytes, DataOutputStream dataOutputStream) throws IOException {
        if (bytes.length > Short.MAX_VALUE)
            throw new IOException(new ArrayIndexOutOfBoundsException("Length too large"));

        dataOutputStream.writeShort(bytes.length);
        dataOutputStream.write(bytes);
    }

    public static final<T> List<T> readList(DataInputStream dataInputStream, DataSerializer<T> serializer) throws IOException {
        int length = dataInputStream.readShort();
        if (length > Short.MAX_VALUE)
            throw new IOException(new ArrayIndexOutOfBoundsException("Length too large"));

        List<T> list = new ArrayList<T>(length);
        for (int i = 0; i < length; i ++)
            list.add(serializer.read(dataInputStream));

        return list;
    }

    public static final<T> void writeList(List<T> list, DataOutputStream dataOutputStream, DataSerializer<T> serializer) throws IOException {
        int length = list.size();
        if (length > Short.MAX_VALUE)
            throw new IOException(new ArrayIndexOutOfBoundsException("Length too large"));

        dataOutputStream.writeShort(length);

        for (int i = 0; i < length; i ++) {
            serializer.write(list.get(i), dataOutputStream);
        }
    }

}
