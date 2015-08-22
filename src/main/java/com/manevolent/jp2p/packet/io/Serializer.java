package com.manevolent.jp2p.packet.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Serializer {
    private static final Serializer DEFAULT = new Serializer(null);
    static {
        DEFAULT.put(String.class, new DataSerializer<String>() {
            @Override
            public String read(DataInputStream dataInputStream) throws IOException {
                return dataInputStream.readUTF();
            }
            @Override
            public void write(String object, DataOutputStream dataOutputStream) throws IOException {
                dataOutputStream.writeUTF(object);
            }
        });
        DEFAULT.put(Float.class, new DataSerializer<Float>() {
            @Override
            public Float read(DataInputStream dataInputStream) throws IOException {
                return dataInputStream.readFloat();
            }
            @Override
            public void write(Float object, DataOutputStream dataOutputStream) throws IOException {
                dataOutputStream.writeFloat(object);
            }
        });
        DEFAULT.put(Double.class, new DataSerializer<Double>() {
            @Override
            public Double read(DataInputStream dataInputStream) throws IOException {
                return dataInputStream.readDouble();
            }
            @Override
            public void write(Double object, DataOutputStream dataOutputStream) throws IOException {
                dataOutputStream.writeDouble(object);
            }
        });
        DEFAULT.put(Boolean.class, new DataSerializer<Boolean>() {
            @Override
            public Boolean read(DataInputStream dataInputStream) throws IOException {
                return dataInputStream.readBoolean();
            }
            @Override
            public void write(Boolean object, DataOutputStream dataOutputStream) throws IOException {
                dataOutputStream.writeBoolean(object);
            }
        });
        DEFAULT.put(Character.class, new DataSerializer<Character>() {
            @Override
            public Character read(DataInputStream dataInputStream) throws IOException {
                return dataInputStream.readChar();
            }
            @Override
            public void write(Character object, DataOutputStream dataOutputStream) throws IOException {
                dataOutputStream.writeChar(object);
            }
        });
        DEFAULT.put(Byte.class, new DataSerializer<Byte>() {
            @Override
            public Byte read(DataInputStream dataInputStream) throws IOException {
                return dataInputStream.readByte();
            }
            @Override
            public void write(Byte object, DataOutputStream dataOutputStream) throws IOException {
                dataOutputStream.writeByte(object);
            }
        });
        DEFAULT.put(Short.class, new DataSerializer<Short>() {
            @Override
            public Short read(DataInputStream dataInputStream) throws IOException {
                return dataInputStream.readShort();
            }
            @Override
            public void write(Short object, DataOutputStream dataOutputStream) throws IOException {
                dataOutputStream.writeShort(object);
            }
        });
        DEFAULT.put(Integer.class, new DataSerializer<Integer>() {
            @Override
            public Integer read(DataInputStream dataInputStream) throws IOException {
                return dataInputStream.readInt();
            }
            @Override
            public void write(Integer object, DataOutputStream dataOutputStream) throws IOException {
                dataOutputStream.writeInt(object);
            }
        });
        DEFAULT.put(Long.class, new DataSerializer<Long>() {
            @Override
            public Long read(DataInputStream dataInputStream) throws IOException {
                return dataInputStream.readLong();
            }
            @Override
            public void write(Long object, DataOutputStream dataOutputStream) throws IOException {
                dataOutputStream.writeLong(object);
            }
        });
        DEFAULT.put(Date.class, new DataSerializer<Date>() {
            @Override
            public Date read(DataInputStream dataInputStream) throws IOException {
                return new Date(dataInputStream.readLong());
            }
            @Override
            public void write(Date object, DataOutputStream dataOutputStream) throws IOException {
                dataOutputStream.writeLong(object.getTime());
            }
        });
    }

    private final Serializer parent;
    private final Map<Class, DataSerializer> serializerMap = new HashMap<>();
    private final Map<String, Class> serializerTypeMap = new HashMap<>();

    protected Serializer(Serializer parent) {
        this.parent = parent;
    }

    public Serializer() {
        this(DEFAULT);
    }

    public void put(Class type, DataSerializer serializer) {
        if (type.isAnonymousClass())
            throw new IllegalArgumentException("Cannot register anonymous class");

        this.serializerMap.put(type, serializer);
        this.serializerTypeMap.put(type.getName(), type);
    }

    public DataSerializer remove(Class type) {
        this.serializerTypeMap.remove(type.getName());
        return this.serializerMap.remove(type);
    }

    public DataSerializer get(Class type) {
        DataSerializer serializer = serializerMap.get(type);
        if (serializer == null) {
            if (parent != null) return parent.get(type);
            else throw new IllegalArgumentException("Unknown class type: " + type.toString());
        } else return serializer;
    }

    public DataSerializer get(String typeName) {
        Class type = serializerTypeMap.get(typeName);
        DataSerializer serializer = type != null ? serializerMap.get(type) : null;
        if (serializer == null) {
            if (parent != null) return parent.get(typeName);
            else throw new IllegalArgumentException("Unknown class type: " + typeName);
        } else return serializer;
    }
}
