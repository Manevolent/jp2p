package com.manevolent.jp2p.client;

import com.google.common.collect.Queues;
import com.manevolent.jp2p.packet.IdentifiedPacket;
import com.manevolent.jp2p.packet.io.PacketOutputStream;

import java.io.IOException;
import java.util.Queue;

public final class ChannelWorker<T extends IdentifiedPacket>
        implements Runnable, PacketOutputStream<T> {

    private ChannelClient<T> socketClient;
    private final Queue<T> packetSendQueue = Queues.newConcurrentLinkedQueue();
    private final boolean clumping;

    public ChannelWorker(ChannelClient<T> socketClient, boolean clumping) {
        this.socketClient = socketClient;
        this.clumping = clumping;
    }

    public ChannelWorker(ChannelClient<T> socketClient) {
        this(socketClient, true);
    }

    @Override
    public void run() {
        try {
            int written = 0;
            while (socketClient.isConnected()) {
                while (socketClient.isReady())
                    socketClient.getPacketHandler().handlePacket(socketClient.readPacket());

                while (packetSendQueue.peek() != null) {
                    socketClient.writePacket(packetSendQueue.remove());

                    if (!clumping)
                        socketClient.flush();

                    written ++;
                }

                if (clumping && written > 0) {
                    socketClient.flush();
                    written = 0;
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void writePacket(T packet) throws IOException {
        if (packet == null)
            throw new NullPointerException("Packet cannot be null");

        if (socketClient.isBlocking())
            throw new UnsupportedOperationException();

        packetSendQueue.add(packet);
    }

    @Override
    public void close() throws IOException {
        //Await the packet queue to be emptied.
        while (packetSendQueue.peek() != null && socketClient.isConnected());

        //Close the socket.
        socketClient.close();
    }

    @Override
    public void flush() throws IOException {
        throw new UnsupportedOperationException();
    }
}
