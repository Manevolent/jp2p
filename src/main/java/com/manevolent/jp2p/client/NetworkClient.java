package com.manevolent.jp2p.client;

import com.manevolent.jp2p.NetworkChannel;
import com.manevolent.jp2p.endpoint.Endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class NetworkClient extends NetworkChannel {

    /**
     * Gets the InputStream of the network socket.
     * @return InputStream.
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Gets the OutputStream of the network socket.
     * @return OutputStream.
     */
    public abstract OutputStream getOutputStream() throws IOException;

    /**
     * Gets the remote Endpoint of the network socket.
     * @return Endpoint.
     */
    public abstract Endpoint getRemoteEndpoint();

    /**
     * Finds the connectivity status of the network socket
     * @return true of the network socket is connected, false otherwise.
     */
    public abstract boolean isConnected();

    /**
     * Finds if this client will block on read or write operations.
     * @return true if te socket will block, false otherwise.
     */
    public abstract boolean isBlocking();

}
