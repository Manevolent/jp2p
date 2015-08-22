package com.manevolent.jp2p.packet.io;

import com.manevolent.jp2p.packet.Packet;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.*;

public class ImageSerializer implements DataSerializer<RenderedImage> {
    private final String format;

    public ImageSerializer(String format) {
        this.format = format;
    }

    public ImageSerializer() {
        this("PNG");
    }

    @Override
    public RenderedImage read(DataInputStream dataInputStream) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Packet.readBytes(dataInputStream));
        return ImageIO.read(inputStream);
    }

    @Override
    public void write(RenderedImage object, DataOutputStream dataOutputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(object, format, outputStream);
        Packet.writeBytes(outputStream.toByteArray(), dataOutputStream);
    }
}
