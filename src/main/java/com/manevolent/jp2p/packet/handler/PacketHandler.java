package com.manevolent.jp2p.packet.handler;

import com.manevolent.jp2p.packet.IdentifiedPacket;
import com.manevolent.jp2p.packet.io.PacketOutputStream;

public abstract class PacketHandler<T extends IdentifiedPacket> {
    private PacketOutputStream<T> packetOutputStream;

    protected PacketOutputStream<T> getOutputStream() {
        return packetOutputStream;
    }

    public void setOutputStream(PacketOutputStream<T> packetOutputStream) {
        this.packetOutputStream = packetOutputStream;
    }

    /**
     * Handles a packet for a socket.
     * @param packet Packet to handle.
     * @throws PacketHandleException
     */
    public abstract void handlePacket(T packet) throws PacketHandleException;

    /**
     * Attempts to ping a remote host.
     * @return true if the last ping was successful, false otherwise.
     */
    public abstract boolean ping();

}
