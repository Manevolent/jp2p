package com.manevolent.jp2p.endpoint.access;

import com.manevolent.jp2p.endpoint.Endpoint;

public abstract class NetworkAccess {

    public abstract boolean canAccess(Endpoint endpoint);

}
