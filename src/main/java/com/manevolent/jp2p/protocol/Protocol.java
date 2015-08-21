package com.manevolent.jp2p.protocol;

import com.manevolent.jp2p.client.NetworkClient;
import com.manevolent.jp2p.packet.IdentifiedPacket;
import com.manevolent.jp2p.packet.PacketFactory;

import java.io.IOException;

public abstract class Protocol<T extends IdentifiedPacket> {
    private final PacketFactory<T> packetFactory;

    public Protocol(PacketFactory<T> packetFactory) {
        this.packetFactory = packetFactory;
    }

    public PacketFactory<T> getPacketFactory() {
        return packetFactory;
    }

    /**
     * Reads a packet from the stream.
     * @return Packet read from the stream.
     * @throws IOException
     */
    public abstract T readPacket(NetworkClient networkClient) throws IOException;

    /**
     * Writes a packet onto the stream.
     * @param packet Packet to write onto the stream.
     * @throws IOException
     */
    public abstract void writePacket(T packet, NetworkClient networkClient) throws IOException;

}
