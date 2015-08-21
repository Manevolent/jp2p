package com.manevolent.jp2p.datagram.reliability;

public class ReliabilityPacket {
    public static int MTU = 1024;

    private byte[] data;
    private boolean acknowledged;

    public ReliabilityPacket(byte[] data) {
        this.data = data;
        this.acknowledged = false;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public byte[] getData() {
        return data;
    }
}
