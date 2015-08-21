package com.manevolent.jp2p.datagram;

import java.io.IOException;

public interface DatagramReceiver {

    Datagram receive() throws IOException;

}
