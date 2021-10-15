package de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared;

import net.jcip.annotations.Immutable;

import java.io.Serializable;

@Immutable
final public class ParseDirectory implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String fileExtension;

    private final String path;

    public ParseDirectory(String path, String fileExtension){
        this.path = path;
        this.fileExtension = fileExtension;
    }

    public String getPath() {
        return path;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String toString() {
        return "Parse directory = [" + path + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (fileExtension.hashCode() ^ (fileExtension.hashCode() >>> 32));
        result = prime * result + (int) (path.hashCode() ^ (path.hashCode() >>> 32));
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
        ParseDirectory other = (ParseDirectory) obj;
        if (fileExtension != other.getFileExtension()) {
            return false;
        }
        if (path != other.getPath()) {
            return false;
        }
        return true;
    }

}
