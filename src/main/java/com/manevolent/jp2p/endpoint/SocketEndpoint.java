package com.manevolent.jp2p.endpoint;

import com.manevolent.jp2p.NetworkChannel;
import com.manevolent.jp2p.NetworkProtocol;

import java.io.IOException;
import java.net.InetAddress;

public abstract class SocketEndpoint extends Endpoint {
    private InetAddress inetAddress;
    private int port;

    public SocketEndpoint(NetworkProtocol networkProtocol, InetAddress address, int port) {
        super(networkProtocol);
        this.inetAddress = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return inetAddress;
    }
    public int getPort() {
        return port;
    }

    /**
     * Connects to the endpoint, creating a new socket in the process.
     * @return Connected socket.
     * @throws IOException
     */
    public abstract NetworkChannel connect() throws IOException;

    @Override
    public String toString() {
        return super.toString() + ":" + inetAddress.getHostAddress() + ":" + port;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (o instanceof SocketEndpoint)
            return ((SocketEndpoint) o).port == port && ((SocketEndpoint) o).inetAddress.equals(inetAddress);
        else
            return false;
    }
}
