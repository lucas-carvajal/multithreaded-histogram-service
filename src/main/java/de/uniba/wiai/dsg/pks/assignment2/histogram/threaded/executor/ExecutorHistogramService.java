package de.uniba.wiai.dsg.pks.assignment2.histogram.threaded.executor;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramService;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.*;

@ThreadSafe
public class ExecutorHistogramService implements HistogramService {

	@GuardedBy(value = "itself")
	private Future<Histogram> result;

	@GuardedBy(value = "itself")
	private ExecutorService masterPool;

	@GuardedBy(value = "itself")
	private Histogram mainHistogram;

	public ExecutorHistogramService() {
		// REQUIRED FOR GRADING - DO NOT REMOVE DEFAULT CONSTRUCTOR
		// but you can add code below
	}

	@Override
	public Histogram calculateHistogram(String rootDirectory, String fileExtension) throws HistogramServiceException {
		if (rootDirectory ==  null || rootDirectory.isEmpty()) {
			throw new HistogramServiceException("Argument Exception");
		}

		if (fileExtension == null || fileExtension.isEmpty()) {
			throw new HistogramServiceException("Argument Exception");
		}

		// initialize ExecutorService
		masterPool = Executors.newSingleThreadExecutor();

		try {
			// call ExecutorMasterThread
			result = masterPool.submit(new ExecutorMasterCallable(rootDirectory, fileExtension));
			mainHistogram = result.get(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			masterPool.shutdownNow();
			result.cancel(true);
			throw new HistogramServiceException("Something wrong by executing total result!");
		} catch (ExecutionException e) {
			HistogramHelper.shutdownAndAwaitTermination(masterPool);
			throw new HistogramServiceException("Something wrong by executing total result!");
		} catch (TimeoutException e) {
			throw new HistogramServiceException("Something wrong by executing total result!");
		} finally {
			HistogramHelper.shutdownAndAwaitTermination(masterPool);
		}

		return mainHistogram;
	}

	@Override
	public String toString() {
		return "ExecutorHistogramService";
	}

}
