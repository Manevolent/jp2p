package com.manevolent.jp2p.datagram.reliability;


public class Sequencer<T> {

    /**
     * Sequencer buffer, initialized in the constructor.
     */
    private volatile T[] buffer;

    /**
     * A numerical caret used to remember what offset (from 0) the last
     * item was inserted into. Basically a look-ahead.
     */
    private volatile int caret = 0;

    /**
     * Gets the offset used to remember how many previous sequenced items
     * have been skipped.
     */
    private volatile long offset = 0;

    /**
     * Creates a new sequencer with the specified maximum buffer length.
     * @param length Maximum look-ahead for this sequencer.
     */
    public Sequencer(int length) {
        buffer = allocate(length);
        setOffset(0);
    }

    private T[] allocate(int length) {
        return (T[]) new Object[length];
    }

    /**
     * Shifts all elements in the sequencer left by the specified amount.
     * @param offset Offset to shift by.
     */
    public void skip(int offset) {
        synchronized (buffer) {
            if (offset > buffer.length)
                throw new IllegalArgumentException("Cannot shift array by a larger size than it assumes.");

            T[] newBuffer = allocate(buffer.length);
            System.arraycopy(buffer, offset, newBuffer, 0, buffer.length - offset);
            System.arraycopy(newBuffer, 0, buffer, 0, buffer.length);

            setCaret(getCaret() - offset);
            setOffset(getOffset() + offset);
        }
    }

    public void skip() {
        skip(1);
    }

    public long getOffset() {
        synchronized (buffer) {
            return offset;
        }
    }

    private void setOffset(long offset) {
        synchronized (buffer) {
            this.offset = Math.max(0L, offset);
        }
    }

    public int getCaret() {
        synchronized (buffer) {
            return caret;
        }
    }

    private void setCaret(int caret) {
        synchronized (buffer) {
            this.caret = Math.max(0, caret);
        }
    }

    public int getSize() {
        synchronized (buffer) {
            return buffer.length;
        }
    }

    public boolean has(long i) {
        synchronized (buffer) {
            return get(i) != null;
        }
    }

    public T put(long i, T o) {
        synchronized (buffer) {
            long ri = i - offset;
            if (ri < 0) throw new IllegalArgumentException("Sequence too small: " + i);
            if (ri >= buffer.length) throw new IllegalArgumentException("Sequence too large: " + i);
            if (o == null) throw new IllegalArgumentException("Cannot set value to null.");
            T old = buffer[(int) ri];
            buffer[(int) ri] = o;
            return old;
        }
    }

    public long put(T o) {
        synchronized (buffer) {
            int caret = getCaret();
            int size = getSize();
            if (caret >= size) throw new IllegalArgumentException("Buffer overflow: " + size);
            long offset = getOffset();

            T old = put(offset + caret, o);
            setCaret(caret + 1);
            return offset + caret;
        }
    }

    public boolean ready() {
        synchronized (buffer) {
            return buffer[0] != null;
        }
    }

    public T next() {
        synchronized (buffer) {
            T i = current();
            skip(1);
            return i;
        }
    }

    public T current() {
        synchronized (buffer) {
            return buffer[0];
        }
    }

    public T get(long i) {
        synchronized (buffer) {
            long ri = i - offset;
            if (ri < 0) return null;
            if (ri >= buffer.length) throw new IllegalArgumentException("Sequence too large: " + i);

            return buffer[(int) ri];
        }
    }

    public int available() {
        synchronized (buffer) {
            return getSize() - getCaret();
        }
    }

    public void offset(long o) {
        synchronized (buffer) {
            this.offset = o;
        }
    }
}
