package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages;

import net.jcip.annotations.Immutable;

@Immutable
public final class ErrorMessage {

    private final String message;
    private final Exception exception;

    public ErrorMessage(String message, Exception exception){
        this.message = message;
        this.exception = exception;
    }

    public String getMessage() {
        return message;
    }

    public Exception getException() {
        return exception;
    }

}
