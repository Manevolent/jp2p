package com.manevolent.jp2p.extensible.packet;

import com.manevolent.jp2p.packet.IdentifiedPacket;

public abstract class BinaryPacket extends IdentifiedPacket {
    public BinaryPacket(byte id) {
        super(id);
    }
}
