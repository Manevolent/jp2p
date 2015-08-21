package com.manevolent.jp2p.datagram;

import com.manevolent.jp2p.endpoint.SocketEndpoint;

public final class Datagram {
    private SocketEndpoint endpoint;
    private byte[] data;

    public Datagram(SocketEndpoint endpoint, byte[] data) {
        this.endpoint = endpoint;
        this.data = data;
    }

    public Datagram(byte[] data) {
        this(null, data);
    }

    /**
     * Finds if the datagram has a destination.
     * @return true if a destination has been provided, false otherwise.
     */
    public boolean hasEndpoint() {
        return endpoint != null && endpoint.getAddress() != null;
    }

    /**
     * Gets the endpoint set for the datagram.
     * @return SocketEndpoint instance.
     */
    public SocketEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Gets the datagram data.
     * @return Byte array containing the data this datagram represents.
     */
    public byte[] getData() {
        return data;
    }
}
