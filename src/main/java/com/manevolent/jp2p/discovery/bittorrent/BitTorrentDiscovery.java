package com.manevolent.jp2p.discovery.bittorrent;

import com.manevolent.jp2p.NetworkProtocol;
import com.manevolent.jp2p.discovery.AsyncDiscovery;
import com.manevolent.jp2p.extensible.endpoint.IpEndpoint;
import com.turn.ttorrent.bcodec.BEValue;
import com.turn.ttorrent.bcodec.BEncoder;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.client.announce.Announce;
import com.turn.ttorrent.client.announce.AnnounceResponseListener;
import com.turn.ttorrent.common.Peer;
import com.turn.ttorrent.common.Torrent;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public class BitTorrentDiscovery extends AsyncDiscovery<BitTorrentEndpoint> implements AnnounceResponseListener {
    private Announce announce;
    private NetworkProtocol protocol;
    private Date lastClean = new Date(System.currentTimeMillis());
    private List<Peer> peerList = new CopyOnWriteArrayList<>();

    public BitTorrentDiscovery(Torrent torrent,
                               NetworkProtocol protocol,
                               InetSocketAddress inetSocketAddress,
                               AsyncDiscoveryCallback<BitTorrentEndpoint> callback)
            throws IOException {
        super(callback);

        this.protocol = protocol;

        Random random = new Random();
        byte[] peerId = new byte[20];
        random.nextBytes(peerId);

        String id = "-TO0042-" + UUID.randomUUID().toString().split("-")[4];
        Peer peer = new Peer(
                inetSocketAddress,
                ByteBuffer.wrap(id.getBytes("ISO-8859-1"))
        );
        this.announce = new Announce(new SharedTorrent(torrent, new File("./")), peer);
        this.announce.register(this);
    }

    @Override
    public List<BitTorrentEndpoint> get() {
        List<BitTorrentEndpoint> endpointList = new ArrayList<>();

        for (Peer peer : peerList) {
            endpointList.add(new BitTorrentEndpoint(
                    protocol,
                    peer
            ));
        }

        return endpointList;
    }

    @Override
    public void handleAnnounceResponse(int interval, int complete, int incomplete) {
        // Do nothing.
    }

    @Override
    public void handleDiscoveredPeers(List<Peer> peers) {
        Date now = new Date(System.currentTimeMillis());
        if (now.getTime() - lastClean.getTime() > 60000) {
            if (peers.size() > 0) {
                this.peerList.clear();
            }

            this.lastClean = now;
        }

        for (Peer a : peers) {
            boolean f = false;
            for (Peer b : peerList) {
                if (a.looksLike(b)) {
                    f = true;
                    break;
                }
            }

            if (!f) {
                getCallback().onDiscovered(new BitTorrentEndpoint(
                        protocol,
                        a
                ));
                peerList.add(0, a); //Insert at position 0 to promote the peer.
            }
        }
    }

    /**
     * Generates a torrent fixed to the identifier provided.
     *
     * @param identifier Identifier to generate a torrent by.
     * @return Generated torrent.
     * @throws IOException
     * @throws InterruptedException
     */
    public static Torrent generateTorrent(byte[] identifier, List<URI> trackers, long creationDate)
            throws IOException, InterruptedException {
        Map<String, BEValue> torrent = new HashMap<>();

        List<BEValue> tiers = new LinkedList<>();

        List<BEValue> tierInfo = new LinkedList<>();
        for (URI trackerURI : trackers)
            tierInfo.add(new BEValue(trackerURI.toString()));
        tiers.add(new BEValue(tierInfo));

        torrent.put("announce-list", new BEValue(tiers));
        torrent.put("creation date", new BEValue(creationDate));

        Map<String, BEValue> info = new TreeMap<>();
        info.put("name", new BEValue(Hex.encodeHexString(identifier)));
        info.put("piece length", new BEValue(20));
        info.put("length", new BEValue(identifier.length));
        info.put("pieces", new BEValue(hash(new ByteArrayInputStream(identifier), 20), Torrent.BYTE_ENCODING));
        torrent.put("info", new BEValue(info));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BEncoder.bencode(new BEValue(torrent), baos);

        return new Torrent(baos.toByteArray(), true);
    }

    @Override
    protected void onStart() {
        announce.start();
    }

    @Override
    protected void onEnd() {
        announce.stop();
    }

    private static class CallableChunkHasher implements Callable<String> {
        private final MessageDigest md;
        private final ByteBuffer data;

        CallableChunkHasher(ByteBuffer buffer) {
            this.md = DigestUtils.getSha1Digest();

            this.data = ByteBuffer.allocate(buffer.remaining());
            buffer.mark();
            this.data.put(buffer);
            this.data.clear();
            buffer.reset();
        }

        @Override
        public String call() throws UnsupportedEncodingException {
            this.md.reset();
            this.md.update(this.data.array());
            return new String(md.digest(), Torrent.BYTE_ENCODING);
        }
    }

    private static String hash(InputStream inputStream, int pieceLength)
            throws InterruptedException, IOException {
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        ByteBuffer buffer = ByteBuffer.allocate(pieceLength);
        List<Future<String>> results = new LinkedList<>();
        StringBuilder hashes = new StringBuilder();

        long length = 0L;
        int pieces = 0;

        length += inputStream.available();

        int step = 10;
        int size = inputStream.available();

        try {
            while (read(buffer, inputStream) > 0) {
                if (buffer.remaining() == 0) {
                    buffer.clear();
                    results.add(executor.submit(new CallableChunkHasher(buffer)));
                }

                if (results.size() >= threads) {
                    pieces += accumulateHashes(hashes, results);
                }

                if (length / (double)size * 100f > step) {
                    step += 10;
                }
            }
        } finally {
            inputStream.close();
        }

        if (buffer.position() > 0) {
            buffer.limit(buffer.position());
            buffer.position(0);
            results.add(executor.submit(new CallableChunkHasher(buffer)));
        }

        pieces += accumulateHashes(hashes, results);

        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(10);
        }

        return hashes.toString();
    }

    public static int read(ByteBuffer byteBuffer, InputStream inputStream) throws IOException {
        byte[] bytes = new byte[byteBuffer.remaining()];
        int len = inputStream.read(bytes);
        if (len <= 0) return len;

        byteBuffer.put(bytes, 0, len);
        return len;
    }

    /**
     * Accumulate the piece hashes into a given {@link StringBuilder}.
     *
     * @param hashes The {@link StringBuilder} to append hashes to.
     * @param results The list of {@link Future}s that will yield the piece
     *	hashes.
     */
    private static int accumulateHashes(StringBuilder hashes,
                                        List<Future<String>> results) throws InterruptedException, IOException {
        try {
            int pieces = results.size();
            for (Future<String> chunk : results) {
                hashes.append(chunk.get());
            }
            results.clear();
            return pieces;
        } catch (ExecutionException ee) {
            throw new IOException("Error while hashing the torrent data!", ee);
        }
    }
}
