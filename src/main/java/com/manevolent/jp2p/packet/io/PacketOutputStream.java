package com.manevolent.jp2p.packet.io;

import com.manevolent.jp2p.packet.IdentifiedPacket;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

public interface PacketOutputStream<T extends IdentifiedPacket> extends Closeable, Flushable {

    /**
     * Writes a packet onto the stream.
     * @param packet Packet to write.
     */
    void writePacket(T packet) throws IOException;

}
