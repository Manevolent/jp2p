package com.manevolent.jp2p.datagram;

import java.io.IOException;

public interface DatagramSender {

    void send(Datagram datagram) throws IOException;

}
