package com.manevolent.jp2p.discovery.bittorrent;

import com.manevolent.jp2p.NetworkProtocol;
import com.manevolent.jp2p.extensible.endpoint.IpEndpoint;
import com.turn.ttorrent.common.Peer;

public class BitTorrentEndpoint extends IpEndpoint {
    private final Peer peer;

    public BitTorrentEndpoint(NetworkProtocol networkProtocol, Peer peer) {
        super(networkProtocol, peer.getAddress(), peer.getPort());

        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
    }
}
