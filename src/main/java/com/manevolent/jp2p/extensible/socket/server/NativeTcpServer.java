package com.manevolent.jp2p.extensible.socket.server;

import com.manevolent.jp2p.client.NetworkClient;
import com.manevolent.jp2p.extensible.socket.client.NativeSocketClient;
import com.manevolent.jp2p.server.NetworkServer;

import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class NativeTcpServer extends NetworkServer {
    private ServerSocket serverSocket;

    public NativeTcpServer(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public NetworkClient accept() throws IOException {
        Socket socket = serverSocket.accept();
        socket.setSoTimeout(15000);
        if (socket instanceof SSLSocket) { //Enable SSL.
            ((SSLSocket) socket).startHandshake();
        }
        return new NativeSocketClient(socket, true);
    }

    @Override
    public boolean isConnected() {
        return !serverSocket.isClosed() && serverSocket.isBound();
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }
}
