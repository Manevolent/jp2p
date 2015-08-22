package com.manevolent.jp2p.buffer;

public interface DelayedBufferObject extends BufferedObject {

    /**
     * Finds if this object is sequenced.
     * @return true if the object is sequenced, false otherwise.
     */
    boolean isSequenced();

    /**
     * Gets the object's sequence.
     * @return Sequence.
     */
    long getSequence();

    /**
     * Sets the object's sequence.
     * @param sequence Sequence.
     */
    void setSequence(long sequence);

    /**
     * Gets the delay of the object, in seconds.
     * @return Delay.
     */
    double getDelay();

    /**
     * Sets the delay of the object.
     * @param delay Delay, in seconds.
     */
    void setDelay(double delay);

    /**
     * Gets the length of the object's media, in seconds. Used to calculate adaptive delay patterns;
     * a value of 0 indicates no delay pattern.
     * @return Length.
     */
    double getLength();

    /**
     * Sets the length of the object's media.
     * @param length Length, in seconds.
     */
    void setLength(double length);

}
