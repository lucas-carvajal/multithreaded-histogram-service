package de.uniba.wiai.dsg.pks.assignment1.histogram.threaded.lowlevel;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramService;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;

public class LowlevelHistogramService implements HistogramService {

	LowlevelMasterThread lowlevelMasterThread;

	public LowlevelHistogramService() {
		// REQUIRED FOR GRADING - DO NOT REMOVE DEFAULT CONSTRUCTOR
		// but you can add code below
	}

	@Override
	public Histogram calculateHistogram(String rootDirectory, String fileExtension) throws HistogramServiceException {

		if (rootDirectory == null || rootDirectory.isEmpty()) {
			throw new HistogramServiceException("Illegal Argument, rootdirectory is empty, please type in a root directory ");
		}

		if (fileExtension == null || fileExtension.isEmpty()) {
			throw new HistogramServiceException("Illegal Argument, fileextension is empty, please type in a fileextension ");
		}

		//  creates Master Thread, starts and joins it. After finishing computation it gets the histogram from the masterThread.
		//  blockingcoefficient for efficient thread-max calculation (has to be between 0-1)
		lowlevelMasterThread = new LowlevelMasterThread("master", rootDirectory, fileExtension, 0.4);
		lowlevelMasterThread.start();

		try {
			lowlevelMasterThread.join();
		} catch (InterruptedException e) {
			lowlevelMasterThread.shutdown();
			throw new HistogramServiceException("Histogram Service interrupted . . .");
		}
		return lowlevelMasterThread.getHistogram();
	}

	@Override
	public String toString() {
		return "LowlevelHistogramService";
	}
}
