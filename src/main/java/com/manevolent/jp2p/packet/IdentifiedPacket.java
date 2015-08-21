package com.manevolent.jp2p.packet;

public abstract class IdentifiedPacket extends Packet implements Identified {
    private final byte id;

    public IdentifiedPacket(byte id) {
        this.id = id;
    }

    /**
     * Gets the packet's ID, or identifier.
     * @return Packet identifier
     */
    public byte getId() {
        return id;
    }
}
