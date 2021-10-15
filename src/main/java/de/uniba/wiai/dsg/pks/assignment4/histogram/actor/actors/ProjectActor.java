package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.actors;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.routing.ActorRefRoutee;
import akka.routing.Broadcast;
import akka.routing.Routee;
import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.ErrorMessage;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.FolderRequestMessage;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.FolderResponseMessage;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.HistogramMessage;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

@NotThreadSafe
public class ProjectActor extends AbstractActor {

    private static final int NUMBER_ACTORS = 4;

    private ActorRef sender;

    public static Props props(String rootDirectory, String fileExtension) {
        return Props.create(ProjectActor.class, () -> new ProjectActor(rootDirectory, fileExtension));
    }

    private final AtomicInteger counter = new AtomicInteger(0);
    private final String rootDirectory;
    private final String fileExtension;
    private final Queue<Path> pathsQueue = new LinkedList<>();
    private Histogram mainHistogram;
    private ActorRef outputActor;
    private ActorRef loadBalancer;

    public ProjectActor(String rootDirectory, String fileExtension) {
        this.rootDirectory = rootDirectory;
        this.fileExtension = fileExtension;
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        mainHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(),0,0,0,0);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FolderResponseMessage.class, this::updateMainHistogram)
                .matchEquals("total result", this::iterateRootPath)
                .matchAny(any -> System.out.println("Receive unknown message: " + any))
                .build();
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        shutdownLoadBalancer();
    }

    private void iterateRootPath(String message) {

        sender =  getSender();

        Path firstPath = Path.of(rootDirectory);
        pathsQueue.add(firstPath);

        outputActor = getContext().actorOf(OutputActor.props());

        // create FileActor nodes and store them in list
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < NUMBER_ACTORS; i++) {
            ActorRef r = getContext().actorOf(Props.create(FileActor.class), "FileActor-" + i);
            routees.add(new ActorRefRoutee(r));
        }

        loadBalancer = getContext().actorOf(Props.create(LoadBalancer.class, routees));
        initNewFolderActor(outputActor, loadBalancer).tell(new FolderRequestMessage(rootDirectory, fileExtension), getSelf());
        counter.incrementAndGet();
        while (pathsQueue.size() > 0) {
            // iterates root directory
            Path rootPath = pathsQueue.poll();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
                // starts a new FolderActor by finding a directory in the rootPath stream
                for (Path path : stream) {
                    if (Files.isDirectory(path)) {
                        pathsQueue.add(path);
                        counter.incrementAndGet();
                        initNewFolderActor(outputActor, loadBalancer).tell(new FolderRequestMessage(path.toString(), fileExtension), getSelf());
                    }
                }
            } catch (IOException e) {
                outputActor.tell(new ErrorMessage("Error by iterating the rootPath: ",
                        e), ActorRef.noSender());
            }
        }
    }

    private ActorRef initNewFolderActor(ActorRef outputActorRef, ActorRef loadBalancer) {
        return getContext().actorOf(FolderActor.props(outputActorRef, loadBalancer));
    }

    private void updateMainHistogram(FolderResponseMessage folderResponseMessage) {
        HistogramHelper.updateMainHistogram(mainHistogram, folderResponseMessage.getHistogramMessage().getHistogram());
        if (counter.get() == 1) {
            // sends MainHistogram to OutputActor
            outputActor.tell(new HistogramMessage(mainHistogram), ActorRef.noSender());
            // sends MainHistogram to Service
            sender.tell(new HistogramMessage(mainHistogram), getSelf());
            shutdownLoadBalancer();
        } else {
            counter.decrementAndGet();
        }
    }

    public void shutdownLoadBalancer() {
        loadBalancer.tell(new Broadcast(PoisonPill.getInstance()), ActorRef.noSender());
        loadBalancer.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return this.initializeSupervisionStrategy();
    }

    private OneForOneStrategy initializeSupervisionStrategy() {
        return new OneForOneStrategy(2, Duration.ofMinutes(1),
                DeciderBuilder
            .match(IOException.class, e -> {
                outputActor.tell(new ErrorMessage("Error while processing file: ",
                        e), ActorRef.noSender());
                return SupervisorStrategy.restart();
            })
            .match(ClassNotFoundException.class, e -> {
                outputActor.tell(new ErrorMessage("Error while processing a request: ",
                        e), ActorRef.noSender());
                return SupervisorStrategy.restart();
            })
            .matchAny(o -> SupervisorStrategy.escalate()).build());
    }

}
