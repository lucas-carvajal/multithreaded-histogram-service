package de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared;

import net.jcip.annotations.Immutable;

import java.io.Serializable;

@Immutable
final public class TerminateConnection implements Serializable {

    private static final long serialVersionUID = 1L;

    public String toString() {
        return "Terminate Connection";
    }

}
