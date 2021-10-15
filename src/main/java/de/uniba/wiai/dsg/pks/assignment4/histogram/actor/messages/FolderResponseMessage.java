package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages;

import net.jcip.annotations.Immutable;

@Immutable
public final class FolderResponseMessage {
    private final HistogramMessage histogramMessage;
    private final String path;

    public FolderResponseMessage(HistogramMessage histogramMessage, String path) {
        this.histogramMessage = histogramMessage;
        this.path = path;
    }

    public HistogramMessage getHistogramMessage() {
        return histogramMessage;
    }

    public String getPath() {
        return path;
    }

}
