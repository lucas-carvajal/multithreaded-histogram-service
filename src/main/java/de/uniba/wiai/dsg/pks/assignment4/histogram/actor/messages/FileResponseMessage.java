package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages;

import net.jcip.annotations.Immutable;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Immutable
public final class FileResponseMessage implements Serializable {

    private final String path;
    private final List<Long> distribution;
    private final int lineCount;

    public FileResponseMessage(List<Long> array, String path, int lineCount){
        distribution = Collections.unmodifiableList(array);
        this.path = path;
        this.lineCount = lineCount;
    }

    public List<Long> getDistribution() {
        return distribution;
    }

    public String getPath() {
        return path;
    }

    public int getLineCount() {
        return lineCount;
    }
}
