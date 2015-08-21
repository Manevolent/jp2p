package com.manevolent.jp2p.extensible.stream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
    private volatile int length;
    private volatile ByteBuffer buf;

    public ByteBufferInputStream(ByteBuffer buf) {
        this.buf = buf;
        this.length = 0;
    }

    public ByteBufferInputStream(int capacity) {
        this(ByteBuffer.allocate(capacity));
    }

    public ByteBufferInputStream() {
        this(1024);
    }

    @Override
    public int available() {
        synchronized (buf) {
            return length;
        }
    }

    public int remaining() {
        synchronized (buf) {
            return buf.remaining() - available();
        }
    }

    @Override
    public int read() throws IOException {
        synchronized (buf) {
            if (length <= 0)
                return -1;

            length--;
            return buf.get() & 0xFF;
        }
    }

    @Override
    public int read(byte[] bytes, int off, int len)
            throws IOException {
        int read = 0;

        for (;;) {
            int remaining = len - read;
            if (remaining <= 0) break;

            int copy = 0;
            while (copy <= 0) copy = Math.min(length, remaining);

            synchronized (buf) {
                byte[] copyBytes = new byte[copy];
                buf.clear();
                buf.get(copyBytes);
                System.arraycopy(copyBytes, 0, bytes, read, copy);

                int after = length - copy;
                if (after > 0) {
                    byte[] afterBytes = new byte[after];
                    buf.get(afterBytes);
                    buf.clear();
                    buf.put(afterBytes);
                }

                buf.clear();

                length -= copy;
                read += copy;
            }
        }

        return read;
    }

    /**
     * Pushes data onto the input stream to be read by the application.
     * @param bytes Bytes to push.
     * @throws BufferOverflowException
     */
    public void push(byte[] bytes) throws BufferOverflowException, IOException {
        ByteArrayInputStream copyStream = new ByteArrayInputStream(bytes);

        for (;;) {
            int write = copyStream.available();
            if (write <= 0) break;

            int remaining = 0;
            while (remaining <= 0) remaining = buf.capacity() - length;

            synchronized (buf) {
                int available = Math.min(write, remaining);

                byte[] remainingBytes = new byte[length];
                buf.clear();
                buf.get(remainingBytes);

                //Clear the buffer and replace the remaining data in its place at position 0.
                buf.clear();
                buf.put(remainingBytes);

                byte[] chunk = new byte[available];
                copyStream.read(chunk);
                buf.put(chunk);
                length += available;

                //Clear the position so all pushed data can be read.
                buf.clear();
            }
        }
    }
}
