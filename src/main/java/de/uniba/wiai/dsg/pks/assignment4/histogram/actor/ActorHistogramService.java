package de.uniba.wiai.dsg.pks.assignment4.histogram.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.pattern.Patterns;
import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramService;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.actors.ProjectActor;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.HistogramMessage;
import net.jcip.annotations.NotThreadSafe;

import java.time.Duration;
import java.util.concurrent.*;

@NotThreadSafe
public class ActorHistogramService implements HistogramService {

	private Histogram mainHistogram;
	private CompletableFuture<Object> resultOfProjectActor;

	public ActorHistogramService() {
		// REQUIRED FOR GRADING - DO NOT REMOVE DEFAULT CONSTRUCTOR
		// but you can add code below
	}

	@Override
	public Histogram calculateHistogram(String rootDirectory,
			String fileExtension) throws HistogramServiceException {
		ActorSystem actorSystem = ActorSystem.create();

		// check if root directory is valid
		if (rootDirectory == null || rootDirectory.isEmpty()) {
			throw new HistogramServiceException("Root Directory must not be null or empty");
		}

		if (fileExtension == null || fileExtension.isEmpty()) {
			throw new HistogramServiceException("File Extension must not be null or empty");
		}

		ActorRef projectActor = actorSystem.actorOf(ProjectActor.props(rootDirectory, fileExtension));

		try {
			// asks for result from ProjectActor
			resultOfProjectActor = Patterns.ask(projectActor, "total result",
					Duration.ofSeconds(100)).toCompletableFuture();

			// receives result from ProjectActor
			Object object = resultOfProjectActor.get();
			if (object instanceof HistogramMessage) {
				HistogramMessage result = (HistogramMessage) resultOfProjectActor.get();
				mainHistogram = result.getHistogram();
			}

		} catch (InterruptedException e) {
			throw new HistogramServiceException("Process interrupted");
		} catch (ExecutionException e) {
			resultOfProjectActor.cancel(true);
			throw new HistogramServiceException("Something wrong while processing request");
		} catch (ClassCastException e) {
			System.err.println("Something wrong while reading a message");
			return new Histogram();
		} finally {
			projectActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
			actorSystem.terminate();
			stopActorSystem(actorSystem);
		}

		return mainHistogram;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	private void stopActorSystem(ActorSystem actorSystem) throws HistogramServiceException {
		try {
			actorSystem.getWhenTerminated().toCompletableFuture().get(1000, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			actorSystem.terminate();
			throw new HistogramServiceException("Something went wrong while trying to stop the system");
		} catch (ExecutionException e) {
			throw new HistogramServiceException("Something wrong by stopping the system" + e.getMessage());
		} catch (TimeoutException e) {
			throw new HistogramServiceException("Timeout while stopping the system " + e.getMessage());
		}
	}

}