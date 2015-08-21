package com.manevolent.jp2p.client;

import com.manevolent.jp2p.extensible.endpoint.IpEndpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Exposes NetworkClient objects as a socket type.
 */
public class NetworkSocket extends Socket {
    private NetworkClient networkClient;

    public NetworkSocket(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    @Override
    public void connect(SocketAddress address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bind(SocketAddress address) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnected() {
        return networkClient.isConnected();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return networkClient.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return networkClient.getOutputStream();
    }

    @Override
    public InetAddress getInetAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPort() {
        return ((IpEndpoint)networkClient.getRemoteEndpoint()).getPort();
    }

    @Override
    public int getLocalPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return new InetSocketAddress(
                ((IpEndpoint)networkClient.getRemoteEndpoint()).getAddress(),
                getPort()
        );
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTcpNoDelay(boolean on) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSoLinger(boolean on, int linger) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendUrgentData (int data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOOBInline(boolean on) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void setSoTimeout(int timeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void setSendBufferSize(int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setKeepAlive(boolean on) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTrafficClass(int tc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReuseAddress(boolean on) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void close() throws IOException {
        networkClient.close();
    }

    @Override
    public void shutdownInput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdownOutput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return networkClient.toString();
    }

    @Override
    public boolean isClosed() {
        return !isConnected();
    }
}
