package com.huwng.alterna.vine;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side cache of active vine connections in the current world.
 */
public class VineClientCache {
    private static final List<VineConnection> connections = new CopyOnWriteArrayList<>();

    public static void setConnections(Collection<VineConnection> newConnections) {
        connections.clear();
        connections.addAll(newConnections);
    }

    public static Collection<VineConnection> getConnections() {
        return Collections.unmodifiableCollection(connections);
    }
}
