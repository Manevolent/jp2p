package com.manevolent.jp2p;

public enum ChannelState {

    /**
     * The socket is disconnected and does not currently hold a connection. The socket may be re-used to connect
     * to another party.
     */
    DISCONNECTED,

    /**
     * A connection may be held and a handshake is likely underway, but only limited layer functionality is present.
     */
    CONNECTING,

    /**
     * A connection is likely held, and layer functionality is present.
     */
    CONNECTED,

    /**
     * The socket is listening for connections.
     */
    LISTENING;

}
