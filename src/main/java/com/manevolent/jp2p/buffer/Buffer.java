package com.manevolent.jp2p.buffer;

import com.manevolent.jp2p.datagram.reliability.Sequencer;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a sequenced buffer.
 */
public class Buffer<T extends BufferedObject> {
    protected final List<T> buffer;
    private final int capacity;

    public Buffer(int capacity) {
        this.buffer = new ArrayList<T>(capacity);
        this.capacity = capacity;
    }

    public boolean isFull() {
        return size() >= getCapacity();
    }

    public int getCapacity() {
        return capacity;
    }

    public void put(List<T> o) {
        for (T x : o) put(x);
    }

    public void put(T o) {
        synchronized (buffer) {
            if (isFull())
                throw new ArrayIndexOutOfBoundsException("Buffer capacity exceeded.");

            buffer.add(o);
        }
    }

    public int size() {
        return buffer.size();
    }

    /**
     * Finds if the buffer has any objects available.
     * @return true if objects are available, false otherwise.
     */
    public boolean has() {
        return buffer.size() > 0;
    }

    /**
     * Gets an object from the buffer.
     * @return
     */
    public T get() {
        synchronized (buffer) {
            if (buffer.size() <= 0) return null;
            else return buffer.remove(0);
        }
    }

    public List<T> getAll() {
        synchronized (buffer) {
            int n = buffer.size();
            List<T> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
                out.add(get());
            return out;
        }
    }
}
