package com.manevolent.jp2p.discovery;

import com.manevolent.jp2p.endpoint.Endpoint;

import java.util.List;

public abstract class Discovery<T extends Endpoint> {

    /**
     * Gets a list of endpoints discovered by this discovery.
     * @return
     */
    public abstract List<T> get();

}
