package com.manevolent.jp2p.client;

import com.manevolent.jp2p.endpoint.Endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NetworkClientStreamed extends NetworkClient {
    private NetworkClient networkClient;

    private InputStream inputStream;
    private OutputStream outputStream;

    public NetworkClientStreamed(NetworkClient networkClient, InputStream inputStream, OutputStream outputStream) {
        this.networkClient = networkClient;

        this.inputStream = inputStream;
        this.outputStream = outputStream;
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
        return null;
    }

    @Override
    public boolean isConnected() {
        return networkClient.isConnected();
    }

    @Override
    public boolean isBlocking() {
        return false;
    }

    @Override
    public void close() throws IOException {
        networkClient.close();
    }
}
