package de.uniba.wiai.dsg.pks.assignment1.histogram.threaded.highlevel;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramService;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.util.Arrays;
import java.util.concurrent.Semaphore;

@ThreadSafe
public class HighlevelHistogramService implements HistogramService {

	@GuardedBy(value = "histogramSemaphore")
	Histogram histogram;
	
	@GuardedBy(value = "itself")
	Semaphore histogramSemaphore;

	public HighlevelHistogramService() {
		// REQUIRED FOR GRADING - DO NOT REMOVE DEFAULT CONSTRUCTOR
		// but you can add code below
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

		// create Histogram and HistogramSemaphore
		histogram = new Histogram(HistogramHelper.initEmptyDistributionArray(),0,0,0,0);
		histogramSemaphore = new Semaphore(1, true);

		// call HighLevelMasterThread
		HighlevelMasterThread masterThread = new HighlevelMasterThread(rootDirectory, fileExtension, histogram, histogramSemaphore);

		try {
			masterThread.start();
			masterThread.join();
		} catch (InterruptedException e) {
			masterThread.interrupt();
			throw new HistogramServiceException("Histogram Service interrupted . . .");
		}

		return histogram;
	}

	@Override
	public String toString() {
		return "HighlevelHistogramService";
	}
}
