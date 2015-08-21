package com.manevolent.jp2p.packet.handler;

public class PacketHandleException extends Exception {
    public PacketHandleException(String message) {
        super(message);
    }
    public PacketHandleException(Exception cause) { super (cause); }
}
