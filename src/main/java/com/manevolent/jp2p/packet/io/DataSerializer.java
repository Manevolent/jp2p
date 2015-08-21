package com.manevolent.jp2p.packet.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface DataSerializer<T> {

    /**
     * Reads the packet data from the stream.
     * @param dataInputStream DataInputStream.
     * @return Deserialized object.
     */
    T read(DataInputStream dataInputStream) throws IOException;

    /**
     * Writes the packet data onto the stream.
     * @param object Object to serialize.
     * @param dataOutputStream DataOutputStream.
     */
    void write(T object, DataOutputStream dataOutputStream) throws IOException;

}
