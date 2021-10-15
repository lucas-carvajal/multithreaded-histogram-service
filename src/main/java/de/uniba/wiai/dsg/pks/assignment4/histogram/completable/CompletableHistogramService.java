package de.uniba.wiai.dsg.pks.assignment4.histogram.completable;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramService;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;

public class CompletableHistogramService implements HistogramService {

	public CompletableHistogramService() {
		// REQUIRED FOR GRADING - DO NOT REMOVE DEFAULT CONSTRUCTOR
		// but you can add code below
	}

	@Override
	public Histogram calculateHistogram(String rootDirectory,
			String fileExtension) throws HistogramServiceException {
		throw new UnsupportedOperationException("Implement here");
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
