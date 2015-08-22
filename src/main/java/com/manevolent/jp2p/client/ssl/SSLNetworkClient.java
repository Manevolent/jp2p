package com.manevolent.jp2p.client.ssl;

import com.manevolent.jp2p.client.NetworkClient;
import com.manevolent.jp2p.endpoint.Endpoint;
import com.manevolent.jp2p.extensible.stream.ByteBufferInputStream;
import com.manevolent.jp2p.extensible.stream.ByteBufferOutputStream;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.*;
import java.nio.ByteBuffer;

public class SSLNetworkClient
        extends NetworkClient
        implements ByteBufferOutputStream.ByteBufferFlushCallback {
    private NetworkClient base;

    private final SSLEngine engine;

    private SSLInputStream inputStream;
    private SSLOutputStream outputStream;

    private ByteBufferInputStream receiveBuffer;
    private ByteBufferInputStream sendBuffer;

    protected SSLNetworkClient(NetworkClient base, SSLEngine engine) throws IOException {
        this.base = base;

        this.engine = engine;

        this.receiveBuffer = new ByteBufferInputStream(engine.getSession().getApplicationBufferSize());
        this.sendBuffer = new ByteBufferInputStream(engine.getSession().getApplicationBufferSize());

        this.inputStream = new SSLInputStream();
        this.outputStream = new SSLOutputStream();

        this.engine.beginHandshake();
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
        return base.getRemoteEndpoint();
    }

    @Override
    public boolean isConnected() {
        return base.isConnected();
    }

    public boolean isValid() {
        return engine.getSession().isValid();
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void close() throws IOException {
        engine.closeOutbound();
        base.close();
    }

    private void handleStreams(SSLEngineResult.HandshakeStatus handshakeStatus) throws IOException {
        synchronized (engine) {
            while ( true ) {
                if (handshakeStatus == null) {
                    if ((handshakeStatus = engine.getHandshakeStatus()) == null)
                        return;
                }

                SSLEngineResult r = null;

                switch (handshakeStatus) {
                    case NEED_WRAP:
                        byte[] send = sendBuffer.flushToArray();
                        ByteBuffer wrapRequest = ByteBuffer.wrap(send);
                        ByteBuffer wrapResponse = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());

                        r = engine.wrap(wrapRequest, wrapResponse);
                        byte[] output = wrapResponse.array();

                        if (r.bytesProduced() > 0) {
                            base.getOutputStream().write(output, 0, r.bytesProduced());
                            base.getOutputStream().flush();
                        }

                        if (r.bytesConsumed() < send.length) {
                            byte[] leftover = new byte[send.length - r.bytesConsumed()];
                            System.arraycopy(send, r.bytesConsumed(), leftover, 0, leftover.length);
                            sendBuffer.push(leftover);
                        }

                        break;
                    case NEED_UNWRAP:
                        // Hang until something comes available.
                        while (receiveBuffer.available() <= 0 &&
                                base.getInputStream().available() <= 0);

                        int available = base.getInputStream().available();
                        byte[] target = new byte[available];
                        if (available > 0) {
                            int n = base.getInputStream().read(target);
                            receiveBuffer.push(target);
                        }

                        byte[] unwrap = receiveBuffer.flushToArray();
                        ByteBuffer unwrapRequest = ByteBuffer.wrap(unwrap);
                        ByteBuffer unwrapResponse = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());

                        r = engine.unwrap(unwrapRequest, unwrapResponse);
                        int unwrapLen = r.bytesProduced(); // User-space incoming data length

                        if (unwrapLen > 0) {
                            byte[] input = new byte[unwrapLen];
                            byte[] arr = unwrapResponse.array();
                            System.arraycopy(arr, 0, input, 0, input.length);
                            inputStream.push(input);
                        }

                        if (r.bytesConsumed() < unwrap.length) {
                            byte[] leftover = new byte[unwrap.length - r.bytesConsumed()];
                            System.arraycopy(unwrap, r.bytesConsumed(), leftover, 0, leftover.length);
                            receiveBuffer.push(leftover);
                        }

                        break;
                    case NEED_TASK:
                        engine.getDelegatedTask().run();
                        handshakeStatus = engine.getHandshakeStatus();
                        break;
                    default:
                        return;
                }

                if (r != null) {
                    switch (r.getStatus()) {
                        case CLOSED:
                            throw new IOException("SSL connection closed");
                    }

                    handshakeStatus = r.getHandshakeStatus();
                    if (handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED ||
                            handshakeStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void flush(byte[] bytes) throws IOException {
        while (!isValid()) handleStreams(null);
        sendBuffer.push(bytes);
        handleStreams(SSLEngineResult.HandshakeStatus.NEED_WRAP);
    }

    class SSLInputStream extends ByteBufferInputStream {
        SSLInputStream() {
            super(engine.getSession().getApplicationBufferSize());
        }

        @Override
        public int read() throws IOException {
            while (!isValid()) handleStreams(null);
            while (available() <= 0)
                handleStreams(SSLEngineResult.HandshakeStatus.NEED_UNWRAP);

            return super.read();
        }

        @Override
        public int read(byte[] bytes, int off, int len)
                throws IOException {
            while (!isValid()) handleStreams(null);
            while (available() <= 0)
                handleStreams(SSLEngineResult.HandshakeStatus.NEED_UNWRAP);

            int x = 0;
            len = Math.min(len, available());
            for (int i = off; i < len; i ++) {
                bytes[i] = (byte) read();
                x++;
            }

            return x == 0 ? -1 : x;
        }
    }

    class SSLOutputStream extends ByteBufferOutputStream {
        public SSLOutputStream() {
            super(engine.getSession().getPacketBufferSize(), SSLNetworkClient.this);
        }
    }
}
