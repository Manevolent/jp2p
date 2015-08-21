package com.manevolent.jp2p.datagram;

import java.io.IOException;

/**
 * Provides datagram access to a socket, wrapping it with sub-interfaces.
 */
public class DatagramSocketWrapper extends DatagramSocket {
    private final DatagramReceiver receiver;
    private final DatagramSender sender;

    public DatagramSocketWrapper(DatagramReceiver receiver, DatagramSender sender) {
        this.receiver = receiver;
        this.sender = sender;
    }

    @Override
    public Datagram receive() throws IOException {
        return receiver.receive();
    }

    @Override
    public void send(Datagram bytes) throws IOException {
        sender.send(bytes);
    }
}
