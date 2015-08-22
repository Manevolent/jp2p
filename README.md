# Java P2P
This project aims to provide a thread-safe P2P network protocol stack and API, implemented in pure Java for applications that want to utilize decentralized communications for purposes such as IM, signaling, low-latency media streams, server-free frameworks, and more. It comes bundled with barchart's UDT, along with a very basic low-latency UDP-based reliability protocol. Wrapping your communications with SSL is also supported. JP2P is designed to provide you with a layered network architecture, so you can easily stack protocols on top of eachother with only a few lines of code.


### Protocol implementations

JP2P allows you to extend its API and develop your own packet-based protocols and create custom handlers for sent and received packets. You can extend the Packet class and override its abstract methods, or alternatively make use of a reflection-based serialized packet object.

The basis for communication in JP2P is the standard DataInputStream and DataOutputStream types. As provided in the JDK, these classes can read and write any Java base type. Throughout different layers of a JP2P protocol stack, one of these types is always presented to you depending on the context of the operation your stack is performing.

```java
public class ExamplePacket extends Packet {
    private String username, password;
    
    @Override
    public void read(DataInputStream dataInputStream) throws IOException {
        username = dataInputStream.readUTF();
        password = dataInputStream.readUTF();
    }

    @Override
    public void write(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeUTF(username);
        dataOutputStream.writeUTF(password);
    }
}
```

```java
BoundPacket boundPacket = new BoundPacket(new Serializer());
boundPacket.setProperty("username", username);
boundPacket.setProperty("password", password);
boundPacket.setProperty("avatar", ImageIO.read(new File("avatar.png")));
```

JP2P also supports custom serialization based on the same stream types.

```java
public class ImageSerializer implements DataSerializer<RenderedImage> {
    @Override
    public RenderedImage read(DataInputStream dataInputStream) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Packet.readBytes(dataInputStream));
        return ImageIO.read(inputStream);
    }

    @Override
    public void write(RenderedImage object, DataOutputStream dataOutputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(object, "PNG", outputStream);
        Packet.writeBytes(outputStream.toByteArray(), dataOutputStream);
    }
}
```

### Sequencers and buffers

Along with extensible protocols and serialization, JP2P features a few of basic protocol helpers that keep datagrams sequenced, and also keep media streams (VoIP, video, etc.) timed properly. Combining these two mechanisms makes a jitter-free audio stream and/or a reliable video livestream, even on UDP-based connections with high varying latency.

```java
Sequencer<AudioFrame> sequencer = new Sequencer<AudioFrame>(10000);
sequencer.put(0, new AudioFrame());
sequencer.put(2, new AudioFrame());
sequencer.put(1, new AudioFrame());
sequencer.put(3, new AudioFrame());
    
// Read the sequences in order: 0, 1, 2, 3...
while (sequencer.ready()) sequencer.next();
```

```java
TimeResolution timeResolution = TimeResolution.NANOSECOND;
AdaptiveDelayedBuffer<AudioFrame> jitterBuffer = new AdaptiveDelayedBuffer<>(10000, TimeResolution.NANOSECOND);
jitterBuffer.setAdaptionSpeed(0.9D);
jitterBuffer.setVariance(4.0D);
jitterBuffer.setMaximumDelay(1.0D);

...

while (jitterBuffer.has())
  AudioFrame frame = jitterBuffer.get();
```

### Peer discovery

JP2P exposes a peer discovery API that makes finding peers inside a network a standard feature of the API. The JAR comes bundled with ttorrent, a BitTorrent API which extends a capability to find peers by using pre-generated torrents for the sole purpose of bootstrapping with other peers in the announcement list.

```java
BitTorrentDiscovery discovery = new BitTorrentDiscovery(
    generateTorrent(identifier, trackers, currentTime),
    NetworkProtocol.DATAGRAM,
    new InetSocketAddress("localhost", 1337),
    new AsyncDiscoveryCallback<BitTorrentEndpoint>() {
        @Override
        public void onDiscovered(BitTorrentEndpoint endpoint) {
           String ip = endpoint.getPeer().getIp();
        }
    }
);
        
discovery.setRunning(true);
```

### Packet handlers

You can also construct your own custom packet handlers by extending the PacketHandler type in a class. Attaching this to a JP2P network client provides you with the ability to process and interpret incoming packets.

```java
public class CustomPacketHandler extends PacketHandler<BoundPacket> {
    @Override
    public void handlePacket(BoundPacket packet) throws PacketHandleException {
        String username = packet.getProperty("username", String.class);
        RenderedImage avatar = packet.getProperty("avatar", RenderedImage.class);
    }
}
```
