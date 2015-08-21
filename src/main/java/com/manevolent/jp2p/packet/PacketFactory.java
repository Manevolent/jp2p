package com.manevolent.jp2p.packet;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Transforms identified packets based on their binary identifier.
 * @param <T> Base packet class to manipulate.
 */
public final class PacketFactory<T extends IdentifiedPacket> {
    private final Map<Byte, Class<T>> packetMap = new HashMap<Byte, Class<T>>();

    public PacketFactory() {
    }

    /**
     * Registers a packet mapping into the factory.
     * @param packetId Packet identifier to map.
     * @param packetClass Packet class to map to the identifier specified.
     */
    public <E extends T> void register(byte packetId, Class<E> packetClass) {
        if (packetMap.containsKey(packetId))
            throw new IllegalArgumentException("Packet already mapped");

        //Check the packet class and ensure it is not anonymous
        if (packetClass.isAnonymousClass())
            throw new IllegalArgumentException("Anonymous class not acceptable");

        //Check the packet class and ensure it can be instantiated, i.e. not abstract.
        if (Modifier.isAbstract(packetClass.getModifiers()))
            throw new IllegalArgumentException("Abstract class not acceptable");

        packetMap.put(packetId, (Class<T>) packetClass);
    }

    /**
     * Registers a packet mapping into the factory.
     * @param packetMap Packet mapping to register.
     */
    public void register(PacketMap<T> packetMap) {
        register(packetMap.getIdentifier(), packetMap.getPacketClass());
    }

    /**
     * Un-registers (or removes) a packet mapping from the factory.
     * @param packetId Packet mapping to remove.
     * @return Packet class removed from the factory.
     */
    public Class<T> unregister(byte packetId) {
        if (!packetMap.containsKey(packetId))
            throw new IllegalArgumentException("Packet identifier not mapped");

        return packetMap.remove(packetId);
    }

    /**
     * Creates a packet from the identifier specified.
     * @param packetId Packet identifier.
     * @return Packet instance.
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public T createFromId(byte packetId) throws IllegalAccessException, InstantiationException {
        Class<T> packetClass = packetMap.get(packetId);
        if (packetClass == null)
            throw new IllegalArgumentException("Packet not found with the identifier specified.");

        return packetClass.newInstance();
    }

    /**
     * Creates a PacketFactory instance from a list of packet maps.
     * @param packetMaps Array of packet maps to register.
     * @param <E> Packet base class to connect a factory from.
     * @return PacketFactory instance.
     */
    public static <E extends IdentifiedPacket> PacketFactory<E> createFromList(PacketMap<E>... packetMaps) {
        PacketFactory<E> packetFactory = new PacketFactory<E>();

        for (PacketMap<E> packetMap : packetMaps)
            packetFactory.register(packetMap);

        return packetFactory;
    }
}
