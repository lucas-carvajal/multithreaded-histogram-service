package de.uniba.wiai.dsg.pks.assignment3.histogram.socket.server;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.ParseDirectory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServerCache implements Cache {

    private final Map<ParseDirectory,Histogram> cache;

    public ServerCache () {
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<Histogram> getCachedResult(ParseDirectory request) {
       return Optional.ofNullable(cache.get(request));
    }

    @Override
    public void putInCache(ParseDirectory request, Histogram result) {
        cache.putIfAbsent(request, result);
    }
}

