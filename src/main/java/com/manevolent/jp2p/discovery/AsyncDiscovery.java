package com.manevolent.jp2p.discovery;

import com.manevolent.jp2p.endpoint.Endpoint;

public abstract class AsyncDiscovery<T extends Endpoint> extends Discovery<T> {
    private final AsyncDiscoveryCallback<T> callback;
    private volatile boolean running;

    protected AsyncDiscovery(AsyncDiscoveryCallback<T> callback) {
        this.callback = callback;
    }

    /**
     * Sets the discovery agent's running status.
     * @param running Running.
     */
    public void setRunning(boolean running) {
        if (this.running != running) {
            if (running)
                onStart();
            else
                onEnd();

            this.running = running;
        }
    }

    public boolean isRunning() {
        return running;
    }

    protected AsyncDiscoveryCallback<T> getCallback() {
        return callback;
    }

    protected abstract void onStart();
    protected abstract void onEnd();

    public interface AsyncDiscoveryCallback<T> {
        void onDiscovered(T endpoint);
    }
}
