package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.ErrorMessage;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.FileResponseMessage;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.FolderResponseMessage;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.HistogramMessage;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class OutputActor extends AbstractActor {

    public static Props props() {
        return Props.create(OutputActor.class, OutputActor::new);
    }

    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public void preStart() {
        log.debug("Starting");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FileResponseMessage.class,this::printFileResponse)
                .match(HistogramMessage.class, this::printMainHistogram)
                .match(FolderResponseMessage.class, this::printFolderHistogram)
                .match(ErrorMessage.class, this::printError)

                .matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }

    private void printMainHistogram(HistogramMessage histogram) {
        log.info("\n" + HistogramHelper.printHistogram(histogram.getHistogram()));
    }

    private void printFileResponse(FileResponseMessage fileResponseMessage) {
        log.info("\n" + fileResponseMessage.getPath() + " finished");
    }

    private void printFolderHistogram(FolderResponseMessage folderResponseMessage) {
        log.info("\n" + folderResponseMessage.getPath() + ":\n" + HistogramHelper.printHistogram(folderResponseMessage.getHistogramMessage().getHistogram()));
    }

    private void printError(ErrorMessage errorMessage){
        log.error(errorMessage.getMessage() + ": " + errorMessage.getException().getMessage());
    }
}
