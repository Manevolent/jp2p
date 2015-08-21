package com.manevolent.jp2p.extensible.endpoint;

import com.barchart.udt.net.NetSocketUDT;
import com.manevolent.jp2p.NetworkProtocol;
import com.manevolent.jp2p.client.NetworkClient;
import com.manevolent.jp2p.endpoint.SocketEndpoint;
import com.manevolent.jp2p.extensible.socket.client.NativeSocketClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class IpEndpoint extends SocketEndpoint {
    public IpEndpoint(NetworkProtocol networkProtocol, InetAddress address, int port) {
        super(networkProtocol, address, port);
    }

    @Override
    public NetworkClient connect() throws IOException {
        switch (getProtocol()) {
            case STREAM:
                return new NativeSocketClient(new Socket(getAddress(), getPort()), false);
            case DATAGRAM:
                Socket socket = new NetSocketUDT();
                socket.connect(new InetSocketAddress(getAddress(), getPort()));
                return new NativeSocketClient(socket, true);
            default:
                throw new UnsupportedOperationException("Protocol not supported");
        }
    }
}
