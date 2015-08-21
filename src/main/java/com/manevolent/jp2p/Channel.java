package com.manevolent.jp2p;

import java.io.Closeable;
import java.io.IOException;

public abstract class Channel implements Closeable {
    private NetworkChannel networkChannel;

    public Channel(NetworkChannel networkChannel) {
        this.networkChannel = networkChannel;
    }

    /**
     * Gets the party this socket is assuming.
     * @return Socket party.
     */
    public abstract SocketParty getParty();

    /**
     * Gets the state of the socket.
     * @return Socket state.
     */
    public ChannelState getState() {
        if (networkChannel == null)
            return ChannelState.DISCONNECTED;

        return networkChannel.isConnected() ? ChannelState.CONNECTED : ChannelState.DISCONNECTED;
    }

    @Override
    public void close() throws IOException {
        networkChannel.close();
    }
}
