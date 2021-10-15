package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages;

import net.jcip.annotations.Immutable;

@Immutable
public final class FileRequestMessage {

    private final String path;
    private final String extension;

    public FileRequestMessage(String path, String extension){
        this.extension = extension;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getExtension() {
        return extension;
    }}