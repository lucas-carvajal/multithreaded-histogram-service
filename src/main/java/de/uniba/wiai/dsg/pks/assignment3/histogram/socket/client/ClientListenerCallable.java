package de.uniba.wiai.dsg.pks.assignment3.histogram.socket.client;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.ReturnResult;
import net.jcip.annotations.NotThreadSafe;

import java.io.ObjectInputStream;
import java.util.concurrent.Callable;

@NotThreadSafe
public class ClientListenerCallable implements Callable<Histogram> {

    private final ObjectInputStream inputStream;

    public ClientListenerCallable(ObjectInputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public Histogram call() throws Exception {
        ReturnResult result;
        // final histogram is ReturnResult
        try {
            // receive ReturnResult from Server
            Object object = inputStream.readObject();
            // cast received object to ReturnResult
            result = (ReturnResult) object;
            System.out.println("A ReturnResult has been sent from Server");
        } catch (ClassCastException e) {
            System.err.println("Could not read response message");
            return new Histogram();
        }

        // return extracted Histogram
        return result.getHistogram();
    }
}
