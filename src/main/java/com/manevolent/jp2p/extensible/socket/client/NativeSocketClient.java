package com.manevolent.jp2p.extensible.socket.client;

import com.manevolent.jp2p.NetworkProtocol;
import com.manevolent.jp2p.client.NetworkClient;
import com.manevolent.jp2p.endpoint.Endpoint;
import com.manevolent.jp2p.extensible.endpoint.IpEndpoint;
import com.manevolent.jp2p.extensible.stream.PositiveInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public final class NativeSocketClient extends NetworkClient {
    private Socket socket;

    private InputStream inputStream;
    private OutputStream outputStream;
    private final boolean blocking;

    public NativeSocketClient(Socket socket, boolean blocking) throws IOException {
        if (!socket.isConnected() || socket.isClosed())
            throw new UnsupportedOperationException("NativeTcpClient requires a connected native socket");

        this.socket = socket;
        this.blocking = blocking;
        this.inputStream = new PositiveInputStream(socket.getInputStream(), socket.getSoTimeout());
        this.outputStream = socket.getOutputStream();
    }

    public InputStream getNativeInputStream() throws IOException {
        return socket.getInputStream();
    }

    public OutputStream getNativeOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public Endpoint getRemoteEndpoint() {
        SocketAddress address = socket.getRemoteSocketAddress();
        if (address == null) throw new NullPointerException("Remote address is null");

        if (address instanceof InetSocketAddress) {
            return new IpEndpoint(
                    NetworkProtocol.STREAM,
                    ((InetSocketAddress) address).getAddress(),
                    ((InetSocketAddress) address).getPort()
            );
        } else
            throw new UnsupportedOperationException("Unknown address: " + address);
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }

    @Override
    public void close() throws IOException {
        outputStream.flush();
        socket.close();
    }
}
