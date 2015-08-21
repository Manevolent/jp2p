package com.manevolent.jp2p.extensible.stream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Used to fix all of the world's stupidity (UDT).
 */
public class PositiveInputStream extends InputStream {
    private final long timeout;
    private final InputStream baseStream;
    private byte[] readBuffer = new byte[1];

    public PositiveInputStream(InputStream inputStream, long timeout) {
        this.timeout = timeout;
        this.baseStream = inputStream;
    }

    @Override
    public int read() throws IOException {
        long start = System.currentTimeMillis();

        while (timeout == 0 || System.currentTimeMillis() - start < timeout) {
            int read = baseStream.read(readBuffer);
            if (read <= 0) continue;

            int compliment = readBuffer[0];
            if (compliment < 0) { //Overflow
                compliment = (Byte.MAX_VALUE - (Byte.MIN_VALUE - compliment) + 1);
            }

            return compliment;
        }

        throw new IOException("Read timed out");
    }
}
