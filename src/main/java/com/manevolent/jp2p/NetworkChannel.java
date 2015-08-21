package com.manevolent.jp2p;

import java.io.Closeable;

public abstract class NetworkChannel implements Closeable {

    /**
     * Finds if this NetworkChannel is connected.
     * @return true if the NetworkChannel is connected, false otherwise.
     */
    public abstract boolean isConnected();

}
