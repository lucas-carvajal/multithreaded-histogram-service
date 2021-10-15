package de.uniba.wiai.dsg.pks.assignment2.histogram.threaded.stream;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramService;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@NotThreadSafe
public class StreamHistogramService implements HistogramService {

	Histogram mainHistogram;
	private ExecutorService outputPool;

	@GuardedBy(value = "itself")
	private final BlockingQueue<String> blockingMessagesQueue;

	public StreamHistogramService() {
		// REQUIRED FOR GRADING - DO NOT REMOVE DEFAULT CONSTRUCTOR
		// but you can add code below
		blockingMessagesQueue = new ArrayBlockingQueue<String>(5000, true);
	}

	@Override
	public Histogram calculateHistogram(String rootDirectory, String fileExtension) throws HistogramServiceException {
		// check rootDirectory and file extension
		if (rootDirectory ==  null || rootDirectory.isEmpty()) {
			throw new HistogramServiceException("Argument Exception");
		}

		if (fileExtension == null || fileExtension.isEmpty()) {
			throw new HistogramServiceException("Argument Exception");
		}

		//start output thread
		outputPool = Executors.newSingleThreadExecutor();
		outputPool.submit(new StreamOutputRunnable(blockingMessagesQueue));

		StreamWorkerClass streamWorkerClass = new StreamWorkerClass(rootDirectory, fileExtension, blockingMessagesQueue, Thread.currentThread());

		try {
			mainHistogram = streamWorkerClass.generateHistogram();
		} catch (RuntimeException e) {
			throw new HistogramServiceException("Stream Histogram Service Interrupted");
		}

		//stop output
		try {
			blockingMessagesQueue.put("DONE");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new HistogramServiceException("blockingMessagesQueue interrupted: " + e);
		}
		HistogramHelper.shutdownAndAwaitTermination(outputPool);

		return mainHistogram;
	}

	@Override
	public String toString() {
		return "StreamHistogramService";
	}
}
