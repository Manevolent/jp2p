package com.manevolent.jp2p;

import com.manevolent.jp2p.buffer.AdaptiveDelayedBuffer;
import com.manevolent.jp2p.buffer.DelayedBuffer;
import com.manevolent.jp2p.buffer.DelayedBufferObject;

import javax.sound.sampled.*;
import java.util.Random;

public class JitterBufferTest {
    public static void main(String[] args) throws LineUnavailableException {
        AdaptiveDelayedBuffer<AudioFrame> frameBuffer = new AdaptiveDelayedBuffer<>(
                1280,
                DelayedBuffer.TimeResolution.NANOSECOND
        );

        AudioFormat format = new AudioFormat(44100f, 16, 1, true, false);

        SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(format);
        sourceDataLine.open();

        TargetDataLine targetDataLine = AudioSystem.getTargetDataLine(format);
        targetDataLine.open();

        long seq = 0;
        final int frames = 5;

        targetDataLine.start();
        sourceDataLine.start();

        Random random = new Random();

        double check = 0D;
        while (true) {
            double now = DelayedBuffer.TimeResolution.NANOSECOND.time();

            if (random.nextDouble() > 0.05d) {
                int sz = frames * format.getFrameSize();
                while (targetDataLine.available() >= sz && !frameBuffer.isFull()) {
                    AudioFrame frame = new AudioFrame(format, seq++);
                    frame.read(targetDataLine, frames);
                    frameBuffer.put(frame);
                }
            } else try {
                Thread.sleep(random.nextInt(1000));
            } catch (Exception e) {
                e.printStackTrace();
            }

            while (frameBuffer.has())
                frameBuffer.get().write(sourceDataLine);

            if (now >= check) {
                System.out.println("Buffer delay: " + (frameBuffer.current() * 1000D) + "ms [" + frameBuffer.size() + " buffered]");
                check = now + 1D;
            }
        }
    }

    static class AudioFrame implements DelayedBufferObject {
        private byte[] bytes;
        private AudioFormat format;
        private long sequence = 0L;
        private double delay = 0D;

        public AudioFrame(AudioFormat format, long sequence) {
            this.format = format;
            this.sequence = sequence;
        }

        public int read(TargetDataLine targetDataLine, int count) {
            int sz = count * format.getFrameSize();
            bytes = new byte[sz];
            return targetDataLine.read(bytes, 0, sz);
        }

        public int write(SourceDataLine sourceDataLine) {
            return sourceDataLine.write(bytes, 0, bytes.length);
        }

        @Override
        public boolean isSequenced() {
            return true;
        }

        @Override
        public long getSequence() {
            return sequence;
        }

        @Override
        public void setSequence(long sequence) {
            this.sequence = sequence;
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
            return (double)format.getSampleRate() /
                    ((double)bytes.length / (double)format.getFrameSize());
        }

        @Override
        public void setLength(double length) {
            throw new UnsupportedOperationException();
        }
    }
}
