package com.manevolent.jp2p.datagram;

import java.io.IOException;

public abstract class DatagramSocket implements DatagramSender, DatagramReceiver {

    public abstract Datagram receive() throws IOException;

    public abstract void send(Datagram bytes) throws IOException;

}
