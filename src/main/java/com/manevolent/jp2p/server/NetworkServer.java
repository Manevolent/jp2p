package com.manevolent.jp2p.server;

import com.manevolent.jp2p.NetworkChannel;
import com.manevolent.jp2p.client.NetworkClient;

import java.io.IOException;

public abstract class NetworkServer extends NetworkChannel {

    /**
     * Accepts a client from the server.
     * @return Accepted client.
     */
    public abstract NetworkClient accept() throws IOException;

}
