package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.Immutable;

import java.util.Arrays;

@Immutable
final public class HistogramMessage {

    public static final int ALPHABET_SIZE = 26;

    private final Histogram histogram;

    public HistogramMessage(Histogram histogram){
        this.histogram = new Histogram(deepCopyLongArray(histogram.getDistribution()),
                (int) histogram.getLines(), (int) histogram.getFiles(),
                (int) histogram.getProcessedFiles(), (int) histogram.getDirectories());
    }

    public Histogram getHistogram() {
        return new Histogram(deepCopyLongArray(histogram.getDistribution()),
                (int) histogram.getLines(), (int) histogram.getFiles(),
                (int) histogram.getProcessedFiles(), (int) histogram.getDirectories());
    }

    public String toString() {
        return "Return result = [" + HistogramHelper.printHistogram(histogram) + "]";
    }

    private long[] deepCopyLongArray(long[] originalArray){
        long[] newArray = new long[ALPHABET_SIZE];
        int counter = 0;
        for (long item : originalArray) {
            newArray[counter] = item;
            counter++;
        }
        return newArray;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (histogram.getDirectories() ^ (histogram.getDirectories() >>> 32));
        result = prime * result + Arrays.hashCode(histogram.getDistribution());
        result = prime * result + (int) (histogram.getFiles() ^ (histogram.getFiles() >>> 32));
        result = prime * result + (int) (histogram.getProcessedFiles() ^ (histogram.getProcessedFiles() >>> 32));
        result = prime * result + (int) (histogram.getLines() ^ (histogram.getLines() >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HistogramMessage other = (HistogramMessage) obj;
        if (histogram.getDirectories() != other.getHistogram().getDirectories())
            return false;
        if (!Arrays.equals(histogram.getDistribution(), other.getHistogram().getDistribution()))
            return false;
        if (histogram.getFiles() != other.getHistogram().getFiles())
            return false;
        if (histogram.getProcessedFiles() != other.getHistogram().getProcessedFiles())
            return false;
        if (histogram.getLines() != other.getHistogram().getLines())
            return false;
        return true;
    }

}
