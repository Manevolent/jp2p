package com.manevolent.jp2p.buffer;

import javax.sound.sampled.*;
import java.util.Random;

/**
 * An adaptive, delayed "jitter" buffer based on the formula from:
 * http://www.embedded.com/design/connectivity/4209798/
 *  Reducing-VoIP-quality-degradation-when-network-conditions-are-unstable
 *
 * @param <T> Buffer data type.
 */
public class AdaptiveDelayedBuffer<T extends DelayedBufferObject> extends DelayedBuffer<T> {
    private volatile double acceptable_delay = 1D;
    private volatile double last_time;
    private volatile double last_delay;
    private volatile double last_variance;
    private volatile double last_setback;

    private volatile double a = 0.998002D; // "? is a parameter that impacts the jitter buffer adaptation speed."
    private volatile double b = 4;         // "? is a factor influencing how important the delay variance is."

    public AdaptiveDelayedBuffer(int capacity, TimeResolution resolution) {
        super(capacity, resolution);
        this.last_delay = 0D;
        this.last_variance = 0D;
    }

    /**
     * Sets the jitter buffer adaption speed. A lower value makes the buffer adapt less frequently to delay
     * variation.
     * @param adaptionSpeed Adaption speed.
     */
    public void setAdaptionSpeed(double adaptionSpeed) {
        this.a = adaptionSpeed;
    }

    /**
     * Sets the variance value. A higher value makes the jitter buffer rely more on packet variance to
     * compute future delay.
     * @param variance Variance.
     */
    public void setVariance(double variance) {
        this.b = variance;
    }

    public void setMaximumDelay(double maximumDelay) {
        if (maximumDelay <= 0D) throw new IllegalArgumentException();
        this.acceptable_delay = maximumDelay;
    }

    public double current() {
        return last_setback;
    }

    @Override
    protected void desequence(T o) {
        if (last_time <= 0D) last_time = resolution.time();
        if (last_delay <= 0D) last_delay = Math.max(0D, o.getDelay());

        double time = resolution.time();
        double n = (time - last_time);
        double delay = a * last_delay + (1D - a) * n;
        double variance = a * last_variance + (1D - a) * Math.abs(delay - n);

        this.last_delay = delay;
        this.last_variance = variance;
        this.last_time = time;

        double setback = Math.max(Math.min(acceptable_delay, delay + b * variance), 0D);
        double playout = time + setback;
        this.last_setback = setback;

        o.setDelay(o.getDelay() + playout);

        super.desequence(o);
    }

    public static void main(String[] args) throws LineUnavailableException {
        TimeResolution timeResolution = TimeResolution.NANOSECOND;
        AdaptiveDelayedBuffer<TestObj> delayedBuffer = new AdaptiveDelayedBuffer<>(10000, timeResolution);
        delayedBuffer.setAdaptionSpeed(0.9D);
        delayedBuffer.setVariance(4);
        delayedBuffer.setMaximumDelay(1d);

        double delay = 0d;
        long seq = -1;
        double lastCheck = 0d;
        double s = 0d;

        AudioFormat audioFormat = new AudioFormat(44100f, 16, 1, true, false);
        TargetDataLine dataLine = AudioSystem.getTargetDataLine(audioFormat);
        dataLine.open();
        dataLine.start();

        SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
        sourceDataLine.open();
        sourceDataLine.start();

        while (true) {
            double time = timeResolution.time();
            if (dataLine.available() >= 128) {
                seq++;
                TestObj testObj = new TestObj(seq);
                dataLine.read(testObj.x, 0, testObj.x.length);
                delayedBuffer.put(testObj);
            }

            if (delayedBuffer.has()) {
                TestObj testObj = delayedBuffer.get();
                s = delayedBuffer.current() / 2D;
                sourceDataLine.write(testObj.x, 0, testObj.x.length);
            }


            if (time >= lastCheck) {
                lastCheck = time + 1D;
                System.out.println(
                        "Average delay: " + s + " seconds (" + (1 / s) + "/sec) [" +
                        delayedBuffer.size() + " buffered]"
                );
            }
        }
    }

    static class TestObj implements DelayedBufferObject {
        long seq;
        double delay;
        double t = TimeResolution.NANOSECOND.time();
        byte[] x = new byte[128];

        public double getDelayed() {
            return getDelay() - t;
        }

        public TestObj(long seq) {
            this.seq = seq;
        }

        @Override
        public boolean isSequenced() {
            return true;
        }

        @Override
        public long getSequence() {
            return seq;
        }

        @Override
        public void setSequence(long sequence) {
            this.seq = sequence;
        }

        @Override
        public double getDelay() {
            return delay;
        }

        @Override
        public void setDelay(double delay) {
            this.delay = delay;
        }

        @Override
        public double getLength() {
            return (128D / 2D) / 44100D;
        }

        @Override
        public void setLength(double length) {

        }
    }
}
