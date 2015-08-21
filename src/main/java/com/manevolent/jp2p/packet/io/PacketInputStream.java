package com.manevolent.jp2p.packet.io;

import com.manevolent.jp2p.packet.IdentifiedPacket;

import java.io.Closeable;
import java.io.IOException;

public interface PacketInputStream<T extends IdentifiedPacket> extends Closeable {

    /**
     * Finds if a packet is available on the stream.
     * @return true if a packet is ready to be received on ths stream.
     */
    boolean isReady() throws IOException;

    /**
     * Reads a packet from the stream.
     * @return Packet read from the stream.
     * @throws IOException
     */
    T readPacket() throws IOException;

}
