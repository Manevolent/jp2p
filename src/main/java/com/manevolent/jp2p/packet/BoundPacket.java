package com.manevolent.jp2p.packet;

import com.manevolent.jp2p.packet.io.Serializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class BoundPacket extends Packet {
    private final Serializer serializer;
    private final Map<String, Object> properties = new HashMap<>();

    public BoundPacket(Serializer serializer) {
        this.serializer = serializer;
    }

    public Object setProperty(String key, Object value) {
        if (value == null)
            return this.properties.remove(key);
        else
            return this.properties.put(key, value);
    }
    public <T> T getProperty(String key, Class<T> type) {
        return (T) this.properties.get(key);
    }
    public Object getProperty(String key) {
        return this.properties.get(key);
    }

    @Override
    public void read(DataInputStream dataInputStream) throws IOException {
        int len = dataInputStream.readInt();

        for (int i = 0; i < len; i ++) {
            String key = dataInputStream.readUTF();
            String className = dataInputStream.readUTF();

            if (properties.put(key, serializer.get(className).read(dataInputStream)) != null)
                throw new IOException("Duplicate bound property: " + key);
        }
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        List<Map.Entry<String, Object>> keyList = new ArrayList<>(properties.entrySet());

        int size = keyList.size();
        dataOutputStream.writeInt(size);
        for (int i = 0; i < size; i ++) {
            Map.Entry<String, Object> entry = keyList.get(i);
            dataOutputStream.writeUTF(entry.getKey());
            Object value = entry.getValue();
            dataOutputStream.writeUTF(value.getClass().getName());
            serializer.get(value.getClass()).write(value, dataOutputStream);
        }
    }
}
