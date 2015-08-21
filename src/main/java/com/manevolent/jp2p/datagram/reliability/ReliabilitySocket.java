package com.manevolent.jp2p.datagram.reliability;

import com.manevolent.jp2p.RFC1071;
import com.manevolent.jp2p.client.NetworkClient;
import com.manevolent.jp2p.datagram.Datagram;
import com.manevolent.jp2p.datagram.DatagramSocket;
import com.manevolent.jp2p.endpoint.Endpoint;
import com.manevolent.jp2p.extensible.socket.NativeDatagramSocket;
import com.manevolent.jp2p.extensible.stream.ByteBufferInputStream;
import com.manevolent.jp2p.extensible.stream.ByteBufferOutputStream;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * A very low latency, sequenced, reliable transport over UDP. Does not at all
 * account for network congestion or other related factors -- instead is very heavily
 * focused on transmission speed and the smallest RTT possible. Also does not guarantee
 * sender identity -- should be done by a higher-level protocol instead.
 */
public class ReliabilitySocket
        extends NetworkClient
        implements ByteBufferOutputStream.ByteBufferFlushCallback,
                    Runnable {

    private static final long WINDOW_TIME = 1000;

    public static final byte PACKET_CONTROL = 0x1; //[Ctl Mode]
    public static final byte PACKET_PUSH = 0x2; //[Psh Mode] [Seq] [Len] [Data..] [Checksum]
    public static final byte PACKET_ACK = 0x3; //[Seq]

    public static final byte PUSH_MODE_SEND = 0x1;
    public static final byte PUSH_MODE_RESEND = 0x2;

    public static final byte CONTROL_MODE_CONNECT = 0x1;
    public static final byte CONTROL_MODE_PING = 0x2;
    public static final byte CONTROL_MODE_DISCONNECT = 0x3;

    private DatagramSocket datagramSocket;
    private Endpoint endpoint;
    private Mode socketMode = Mode.WAITING;

    private long remotePeerId = 0;
    private long localPeerId = (new Random()).nextLong();

    /**
     * We keep a remote sequencer to remember remote sequences.
     */
    private Sequencer<ReliabilityPacket> remoteSequence = new Sequencer<ReliabilityPacket>(1024);

    /**
     * We keep a local sequence to remember where we are in our send
     * operations.
     */
    private Sequencer<ReliabilityPacket> localSequence = new Sequencer<ReliabilityPacket>(1024);

    /**
     * Keeps track of the last time (in milliseconds) any data was pushed, or
     * the last time any ping was sent, whichever happened last will be the
     * time represented by this field.
     */
    private long lastSend;

    //The following streams are for higher-level use:
    private ByteBufferInputStream inputStream;
    private ByteBufferOutputStream outputStream;

    public ReliabilitySocket(DatagramSocket datagramSocket, Endpoint endpoint) {
        this.datagramSocket = datagramSocket;
        this.endpoint = endpoint;

        inputStream = new ByteBufferInputStream(1024);
        outputStream = new ByteBufferOutputStream(this);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    public Endpoint getRemoteEndpoint() {
        return endpoint;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isBlocking() {
        return false;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();

        outputStream.flush();
        outputStream.close();
    }

    /**
     * Finds if the stream is currently blocked, meaning all available egress sequence buckets have been filled.
     * @return true if the socket will block on flush, false otherwise.
     */
    public boolean isBlocked() {
        return localSequence.available() <= 0;
    }

    @Override
    public void flush(byte[] bytes) throws IOException {
        if (bytes.length <= 0) throw new IOException("Cannot flush empty array");

        //Write the data, wrapping it around the MTU.
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        for (;;) {
            //Find the next transfer unit bytes to push.
            int len = Math.min(inputStream.available(), ReliabilityPacket.MTU);
            if (len <= 0) break; //Nothing left to send.

            //Wait for the socket to have data ready on the stream.
            while (isBlocked());

            byte[] data = new byte[len];
            inputStream.read(data);

            //Put this data into the sequencer so it can be managed by the network thread.
            long seq = localSequence.put(new ReliabilityPacket(data));
            sendPush(seq, PUSH_MODE_SEND, data, data.length);
        }
    }

    /**
     * Implements the reliability algorithm for this socket. To keep the connection running,
     * there should be a separate thread running elsewhere that continually sends control
     * messages to ping this socket.
     */
    @Override
    public void run() {
        while (isConnected()) {
            try {
                Datagram packet = datagramSocket.receive();

                ByteArrayInputStream packetInputStream = new ByteArrayInputStream(packet.getData());
                DataInputStream dataInputStream = new DataInputStream(packetInputStream);

                long peerId = dataInputStream.readLong();
                if (socketMode == Mode.WAITING) this.remotePeerId = peerId;
                else if (peerId == 0 || this.remotePeerId != peerId) continue;

                byte packetId = dataInputStream.readByte();
                if (packetId == PACKET_CONTROL) {
                    byte controlMode = dataInputStream.readByte();
                    switch (controlMode) {
                        case CONTROL_MODE_CONNECT:
                            if (socketMode == Mode.WAITING) socketMode = Mode.CONNECTED;
                            break;
                        case CONTROL_MODE_DISCONNECT:
                            if (socketMode == Mode.CONNECTED) socketMode = Mode.WAITING;
                            break;
                        case CONTROL_MODE_PING:
                            //Do nothing
                            break;
                        default:
                            break;
                    }
                } else if (packetId == PACKET_PUSH) {
                    byte mode = dataInputStream.readByte();
                    long sequence = dataInputStream.readLong();
                    if (sequence < 0) throw new IOException("Invalid sequence: " + sequence);
                    long checksum = dataInputStream.readLong();

                    int length = dataInputStream.readInt();
                    if (length <= 0 || length > ReliabilityPacket.MTU) throw new IOException("Packet length invalid.");

                    byte[] data = new byte[length];
                    int len = dataInputStream.read(data);
                    if (len != length) throw new IOException("Transport read error: " + len +
                            " read, expected " + length + " (unit: " + packet.getData().length + ").");

                    if (checksum != RFC1071.calculateChecksum(data))
                        throw new IOException("Invalid checksum: " + checksum);

                    sendAck(sequence); //Acknowledge the data

                    ReliabilityPacket readPacket = new ReliabilityPacket(data);
                    readPacket.setAcknowledged(true);
                    if (remoteSequence.getOffset() > sequence)
                        continue;
                    else if (remoteSequence.has(sequence))
                        continue;

                    readPacket.setAcknowledged(true);
                    remoteSequence.put(sequence, readPacket);

                    //Now see what remote packets can be forwarded to the upper-level stream:
                    while (remoteSequence.ready()) ingest(remoteSequence.next());
                } else if (packetId == PACKET_ACK) {
                    long sequence = dataInputStream.readLong();
                    if (localSequence.getOffset() > sequence)
                        continue;

                    ReliabilityPacket ackedPacket = localSequence.get(sequence);
                    if (ackedPacket == null)
                        continue; //Potential re-acknowledgement.
                    else if (ackedPacket.isAcknowledged())
                        continue; //Potential re-acknowledgement.

                    ackedPacket.setAcknowledged(true);

                    //Now see what local packets can be forgotten about:
                    while (localSequence.ready()) {
                        if (localSequence.current().isAcknowledged()) {
                            localSequence.skip();
                        } else {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Ingests a packet into the provided application-layer stream.
     * @param packet Packet to ingest.
     * @throws IOException
     */
    private void ingest(ReliabilityPacket packet) throws IOException {
        if (packet == null) return;

        byte[] data = packet.getData();
        if (data.length <= 0) throw new IOException("Data length invalid.");

        ByteArrayInputStream copyStream = new ByteArrayInputStream(data);
        while (copyStream.available() > 0) {
            int copyReady = copyStream.available();
            if (copyReady <= 0) break;

            int inputReady = 0;
            while (inputReady <= 0) inputReady = inputStream.remaining();

            int available = Math.min(inputReady, copyReady);

            byte[] copyData = new byte[available];
            copyStream.read(copyData, 0, available);
            inputStream.push(copyData);
        }
    }

    /**
     * Updates the socket.
     * @throws IOException
     */
    public void update() throws IOException {
        long time = System.currentTimeMillis();
        if (time - lastSend >= WINDOW_TIME) {
            this.lastSend = System.currentTimeMillis();
            if (!resend()) sendControl(CONTROL_MODE_PING);
        }
    }

    //
    // Network operations
    //

    public void sendPush(long sequence, byte mode, byte[] data, int length) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        //PUSH header
        dataOutputStream.writeLong(localPeerId);
        dataOutputStream.writeByte(PACKET_PUSH);

        //PUSH body
        dataOutputStream.writeByte(mode);
        dataOutputStream.writeLong(sequence);
        dataOutputStream.writeLong(RFC1071.calculateChecksum(data));
        dataOutputStream.writeInt(length);
        dataOutputStream.write(data, 0, length);

        send(outputStream.toByteArray());

        this.lastSend = System.currentTimeMillis();
    }

    public boolean resend() throws IOException {
        return resendPush(localSequence.getOffset());
    }

    public boolean resendPush(long sequence) throws IOException {
        if (localSequence.getSize() <= 0)
            return false;

        for (long offset = 0; offset < localSequence.getSize(); offset ++) {
            ReliabilityPacket packet = localSequence.get(sequence + offset);
            if (packet == null) break;
            else if (!packet.isAcknowledged()) {
                byte[] data = packet.getData();
                sendPush(sequence + offset, PUSH_MODE_RESEND, data, data.length);
            }
        }

        return true;
    }

    public void sendAck(long sequence) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        //ACK header
        dataOutputStream.writeLong(localPeerId);
        dataOutputStream.writeByte(PACKET_ACK);

        //ACK body
        dataOutputStream.writeLong(sequence);

        send(outputStream.toByteArray());
    }

    public void sendControl(byte mode) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        //CONTROL header
        dataOutputStream.writeLong(localPeerId);
        dataOutputStream.writeByte(PACKET_CONTROL);

        //CONTROL body
        dataOutputStream.writeByte(mode);

        send(outputStream.toByteArray());
    }

    public void initialize() throws IOException {
        sendControl(CONTROL_MODE_CONNECT);
    }

    public long getLocalSequence() {
        return localSequence.getOffset();
    }

    public long getRemoteSequence() {
        return remoteSequence.getOffset();
    }

    public void send(byte[] bytes) throws IOException {
        datagramSocket.send(new Datagram(bytes));
    }

    public enum Mode {
        WAITING,
        CONNECTED
    }
}
