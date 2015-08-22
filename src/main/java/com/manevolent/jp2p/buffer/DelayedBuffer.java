package com.manevolent.jp2p.buffer;

import com.manevolent.jp2p.datagram.reliability.Sequencer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class DelayedBuffer<T extends DelayedBufferObject> extends Buffer<T> {
    private static final double MILLISECOND = 1000D;
    private static final double NANOSECOND = 1000000000D;

    private final Sequencer<T> sequencer;
    protected final List<T> backBuffer;

    protected final TimeResolution resolution;
    protected volatile double start; // Start of the buffer.
    protected volatile double last; // Last time a packet was sequenced.
    protected volatile double offset = 0D; // Buffer internal time offset.

    public DelayedBuffer(int capacity, TimeResolution resolution) {
        super(capacity);

        this.backBuffer = new ArrayList<>(capacity);
        this.sequencer = new Sequencer<T>(capacity);

        this.resolution = resolution;
        this.last = this.start = resolution.time();
    }

    protected void consume() {
        synchronized (backBuffer) {
            while (sequencer.ready())
                desequence(sequencer.next());

            int size = backBuffer.size();
            if (size <= 0) return;

            double time = resolution.time() + getOffset();
            for (int i = 0; i < size; i++) {
                T object = backBuffer.get(i);
                if (object.getDelay() <= time) {
                    super.put(backBuffer.remove(i));
                    size = backBuffer.size();
                    i--;
                    continue;
                }

                // Future packets are not supposed to be delayed before this one.
                break;
            }
        }
    }

    protected void desequence(T o) {
        backBuffer.add(o);
    }

    /**
     * Gets the buffer delay offset, in seconds.
     * @return Buffer offset.
     */
    public double getOffset() {
        return offset;
    }

    /**
     * Sets the buffer delay offset, in seconds.
     * @param offset Buffer offset.
     */
    public void setOffset(double offset) {
        this.offset = offset;
    }

    public void put(T o) {
        synchronized (backBuffer) {
            if (isFull())
                throw new ArrayIndexOutOfBoundsException("Buffer capacity exceeded.");

            // Attempt to consume this immediately, otherwise, just put it in the back-buffer.
            double time = resolution.time() + getOffset();
            if (o.getDelay() > 0D && o.getDelay() > time)
                backBuffer.add(o); // Bypass desequencer.
            else {
                if (o.isSequenced()) {
                    sequencer.put(o.getSequence(), o);
                    while (sequencer.ready())
                        desequence(sequencer.next());
                } else desequence(o);
            }
        }
    }

    /**
     * Finds if any objects are pending read.
     * @return true if objects are pending read, false otherwise.
     */
    public boolean isDelayed() {
        consume();
        return backBuffer.size() > 0;
    }

    @Override
    public int size() {
        return super.size() + backBuffer.size() + (sequencer.getSize() - sequencer.available());
    }

    @Override
    public boolean has() {
        consume();
        return super.has();
    }

    @Override
    public T get() {
        consume();
        return super.get();
    }

    @Override
    public List<T> getAll() {
        consume();

        int n = super.size();
        List<T> out = new ArrayList<>(n);
        for (int i = 0; i < n; i ++)
            out.add(super.get());
        return out;
    }

    public enum TimeResolution {
        MILLISECOND(1D / DelayedBuffer.MILLISECOND, new Callable<Double>() {
            @Override
            public Double call() throws Exception {
                return (double)System.currentTimeMillis() / DelayedBuffer.MILLISECOND;
            }
        }),

        NANOSECOND(1D / DelayedBuffer.NANOSECOND, new Callable<Double>() {
            @Override
            public Double call() throws Exception {
                return (double)System.nanoTime() / DelayedBuffer.NANOSECOND;
            }
        });

        private final double resolution;
        private final Callable<Double> method;

        TimeResolution(double resolution, Callable<Double> method) {
            this.resolution = resolution;
            this.method = method;
        }

        /**
         * Gets the resolution, in seconds.
         * @return Absolute resolution.
         */
        public double getResolution() {
            return resolution;
        }

        /**
         * Gets the current time, in seconds.
         * @return Current time if the operation was successful, -1 otherwise.
        */
        public double time() {
            try {
                return method.call();
            } catch (Exception ignored) {
                return -1D;
            }
        }
    }
}
