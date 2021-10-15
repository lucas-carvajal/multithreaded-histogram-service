package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages;

import net.jcip.annotations.Immutable;

@Immutable
public final class FolderRequestMessage {

    private final String path;
    private final String fileExtension;

    public FolderRequestMessage(String path, String extension){
        this.fileExtension = extension;
        this.path = path;
    }

    public String getExtension() {
        return fileExtension;
    }

    public String getPath() {
        return path;
    }
}
