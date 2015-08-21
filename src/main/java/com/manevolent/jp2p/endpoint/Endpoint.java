package com.manevolent.jp2p.endpoint;

import com.manevolent.jp2p.NetworkProtocol;

public abstract class Endpoint {
    private NetworkProtocol networkProtocol;

    public Endpoint(NetworkProtocol networkProtocol) {
        this.networkProtocol = networkProtocol;
    }

    public NetworkProtocol getProtocol() {
        return networkProtocol;
    }

    @Override
    public String toString() {
        return networkProtocol.name();
    }
}
