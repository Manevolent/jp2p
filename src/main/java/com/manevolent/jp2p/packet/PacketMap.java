package com.manevolent.jp2p.packet;


public final class PacketMap<T extends IdentifiedPacket> {
    private byte packetId;
    private Class<T> packet;

    public PacketMap(byte packetId, Class<T> packet) {
        this.packetId = packetId;
        this.packet = packet;
    }

    public byte getIdentifier() {
        return packetId;
    }

    public Class<T> getPacketClass() {
        return packet;
    }

}
