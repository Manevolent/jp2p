package com.manevolent.jp2p.extensible.socket;

import com.manevolent.jp2p.datagram.Datagram;
import com.manevolent.jp2p.datagram.DatagramSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.Random;

public class NativeDatagramSocket extends DatagramSocket {
    private final static Random r = new Random();
    private byte[] buffer;
    private java.net.DatagramSocket datagramSocket;

    public NativeDatagramSocket(java.net.DatagramSocket datagramSocket, int capacity) {
        this.datagramSocket = datagramSocket;

        if (capacity <= 0) throw new IllegalArgumentException("Capacity too small: " + capacity);
        this.buffer = new byte[capacity];
    }

    @Override
    public Datagram receive() throws IOException {
        synchronized (this) {
            DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
            datagramSocket.receive(packet);
            return new Datagram(packet.getData());
        }
    }

    @Override
    public void send(Datagram bytes) throws IOException {
        byte[] data = bytes.getData();
        if (data.length > buffer.length) throw new IOException("Data too long: " + data.length);
        datagramSocket.send(new DatagramPacket(data, data.length));
    }

    public static final NativeDatagramSocket create(int capacity) throws SocketException {
        return new NativeDatagramSocket(
                new java.net.DatagramSocket(
                        r.nextInt(65535 -  49152) +  49152 //IANA "ephemeral port" region for private or dynamic ports.
                ),
                capacity
        );
    }
}
