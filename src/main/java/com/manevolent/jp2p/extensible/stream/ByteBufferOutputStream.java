package com.manevolent.jp2p.extensible.stream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream extends OutputStream {
    private volatile ByteBuffer buf;
    private volatile int length = 0;

    private ByteBufferFlushCallback flushCallback;

    public ByteBufferOutputStream(ByteBuffer buf, ByteBufferFlushCallback flushCallback) {
        this.buf = buf;
        this.flushCallback = flushCallback;
    }

    public ByteBufferOutputStream(ByteBufferFlushCallback flushCallback) {
        this(ByteBuffer.allocate(1024), flushCallback);
    }

    public ByteBufferOutputStream(int capacity, ByteBufferFlushCallback flushCallback) {
        this(ByteBuffer.allocate(capacity), flushCallback);
    }

    @Override
    public void write(int b) throws IOException {
        synchronized (buf) {
            write(new byte[] { (byte) b }, 0, 1);
        }
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        synchronized (buf) {
            byte[] cpy = new byte[len];
            System.arraycopy(bytes, off, cpy, 0, len);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(cpy);
            inputStream.skip(off); //Skip to the offset.

            for (;;) {
                int write = inputStream.available();
                if (write <= 0) break; //We have completed the write.

                //Find the amount of bytes the buffer is ready to consume:
                int available = Math.min(write, buf.remaining());
                if (available <= 0) //We know write > 0, so our buffer's remaining size must be <= 0.
                    flush();
                else { //We are fine, for now, to write transparently to the buffer.
                    byte[] chunk = new byte[available];
                    inputStream.read(chunk);
                    buf.put(chunk, 0, available);
                    length += available;
                }
            }
        }
    }

    @Override
    public void flush() throws IOException {
        //Don't flush an empty buffer.
        if (length <= 0) return;

        byte[] flushBytes = new byte[length];

        synchronized (buf) {
            if (length > 0) {
                //Clear the buffer, or reset its position to 0.
                buf.clear();

                //Fully empty the buffer into an array.
                buf.get(flushBytes);
                buf.clear();

                //Reset the length of the stream to 0.
                length = 0;
            }
        }

        flushCallback.flush(flushBytes);
    }

    public interface ByteBufferFlushCallback {
        public void flush(byte[] bytes) throws IOException;
    }
}
