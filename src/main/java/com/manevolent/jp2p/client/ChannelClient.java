package com.manevolent.jp2p.client;

import com.manevolent.jp2p.Channel;
import com.manevolent.jp2p.ChannelState;
import com.manevolent.jp2p.SocketParty;
import com.manevolent.jp2p.endpoint.Endpoint;
import com.manevolent.jp2p.packet.IdentifiedPacket;
import com.manevolent.jp2p.packet.handler.PacketHandler;
import com.manevolent.jp2p.packet.io.PacketInputStream;
import com.manevolent.jp2p.packet.io.PacketOutputStream;
import com.manevolent.jp2p.protocol.Protocol;

import java.io.IOException;

public class ChannelClient<T extends IdentifiedPacket>
        extends Channel
        implements PacketInputStream<T>, PacketOutputStream<T> {

    private NetworkClient networkClient;
    private Protocol<T> packetProtocol;
    private PacketHandler<T> packetHandler;
    private ChannelWorker<T> clientWorker;

    public ChannelClient(NetworkClient networkClient,
                         Protocol<T> packetProtocol) {
        super(networkClient);

        this.networkClient = networkClient;
        this.packetProtocol = packetProtocol;
        this.clientWorker = createWorker();
    }

    @Override
    public SocketParty getParty() {
        return SocketParty.CLIENT;
    }

    /**
     * Finds the connectivity status of this socket.
     * @return True if the socket is connected, false otherwise.
     */
    public boolean isConnected() {
        return getState() != ChannelState.DISCONNECTED;
    }

    /**
     * Gets the packet handler associated with this socket.
     * @return PacketHandler instance.
     */
    public PacketHandler<T> getPacketHandler() {
        return packetHandler;
    }

    /**
     * Gets the remote endpoint associated with this socket.
     * @return Endpoint instance.
     */
    public Endpoint getRemoteEndpoint() { return networkClient.getRemoteEndpoint(); }

    /**
     * Sets the packet handler associated with this socket.
     * @param packetHandler PacketHandler instance to set.
     */
    public void setPacketHandler(PacketHandler<T> packetHandler) {
        if (packetHandler == null)
            throw new NullPointerException("Packet handler cannot be null");

        this.packetHandler = packetHandler;
        this.packetHandler.setOutputStream(networkClient.isBlocking() ? this : clientWorker); //Associate the packet handler.
    }

    @Override
    public void flush() throws IOException {
        networkClient.getOutputStream().flush();
    }

    @Override
    public boolean isReady() throws IOException {
        if (networkClient.isBlocking())
            return true;
        else
            return networkClient.getInputStream().available() > 0;
    }

    @Override
    public T readPacket() throws IOException {
        synchronized (networkClient.getInputStream()) {
            return packetProtocol.readPacket(networkClient);
        }
    }

    @Override
    public void writePacket(T packet) throws IOException {
        synchronized (networkClient.getOutputStream()) {
            packetProtocol.writePacket(packet, networkClient);
        }
    }

    public void disconnect() throws IOException {
        synchronized (networkClient.getOutputStream()) {
            networkClient.close();
        }
    }

    /**
     * Gets the worker associated with this socket client.
     * @return SocketClientWorker instance.
     */
    public ChannelWorker<T> getWorker() {
        return clientWorker;
    }

    /**
     * Creates a worker class based on this socket.
     * @return Socket worker class.
     */
    private ChannelWorker<T> createWorker() {
        return new ChannelWorker<T>(this);
    }

    /**
     * Finds if this client is blocking.
     * @return true if the client is blocking, false otherwise.
     */
    public boolean isBlocking() {
        return networkClient.isBlocking();
    }

}
