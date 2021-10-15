package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.actors;

import akka.actor.*;
import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.*;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

@NotThreadSafe
public class FolderActor extends AbstractActor {

    private final ActorRef outputActorRef;
    private final ActorRef loadBalancer;
    private final long[] distribution = HistogramHelper.initEmptyDistributionArray();
    private final AtomicInteger counter = new AtomicInteger(0);
    private int fileCounter = 0;
    private int processedFileCounter = 0;
    private int lines = 0 ;
    private FolderRequestMessage requestMessage;

    public FolderActor(ActorRef outputActor, ActorRef loadBalancer) {
        this.outputActorRef = outputActor;
        this.loadBalancer = loadBalancer;
    }

    public static Props props(ActorRef outputActorRef, ActorRef loadBalancer) {
        return Props.create(FolderActor.class, () -> new FolderActor(outputActorRef, loadBalancer));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FolderRequestMessage.class, this::iterateFolderPath)
                .match(FileResponseMessage.class, this::updateFolderHistogram)
                .matchAny(any -> System.out.println("Received unknown message: " + any))
                .build();
    }

    private void iterateFolderPath(FolderRequestMessage requestMessage) {
        this.requestMessage = requestMessage;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(requestMessage.getPath()))) {
            for (Path path : stream) {
               if (Files.isRegularFile(path)) {
                        fileCounter++;
                        if (path.getFileName().toString().endsWith((requestMessage.getExtension()))) {
                            loadBalancer.tell(new FileRequestMessage(path.toString(), requestMessage.getExtension()),getSelf());
                            counter.incrementAndGet();
                            processedFileCounter++;
                        }
                    }
                }
        } catch (IOException e) {
            outputActorRef.tell(new ErrorMessage("Error while processing folder " + requestMessage.getPath(),
                    e), ActorRef.noSender());
        }
        if (counter.get() == 0){
            sendResult();
        }
    }

    private void updateFolderHistogram(FileResponseMessage fileResponseMessage) {
        lines = lines + fileResponseMessage.getLineCount();
        for (int i = 0; i < distribution.length; i++) {
            distribution[i] = distribution[i] + fileResponseMessage.getDistribution().get(i);
        }

        // sends processed File info to OutputActor
        outputActorRef.tell(fileResponseMessage, ActorRef.noSender());

        if (counter.decrementAndGet() == 0) {
            sendResult();
        }
    }

    private void sendResult() {
        Histogram histogram = new Histogram(distribution,lines,fileCounter,processedFileCounter,1);
        HistogramMessage parseHistogram = new HistogramMessage(histogram);

        FolderResponseMessage folderResponseMessage = new FolderResponseMessage(parseHistogram, requestMessage.getPath());

        // sends FolderHistogram to OutputActor
        outputActorRef.tell(folderResponseMessage,ActorRef.noSender());

        getContext().getParent().tell(folderResponseMessage,ActorRef.noSender());
        self().tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

}


