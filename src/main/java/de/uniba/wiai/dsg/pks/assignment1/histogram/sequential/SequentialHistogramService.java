package de.uniba.wiai.dsg.pks.assignment1.histogram.sequential;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramService;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@NotThreadSafe
public class SequentialHistogramService implements HistogramService {

	private int currentPrintedLine;
	private final Histogram globalHistogram;

	public SequentialHistogramService() {
		// REQUIRED FOR GRADING - DO NOT REMOVE DEFAULT CONSTRUCTOR
		// but you can add code below
		this.globalHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(),0,0,0,1);
		this.currentPrintedLine =1;
	}

	@Override
	public Histogram calculateHistogram(String rootDirectory, String fileExtension) throws HistogramServiceException {

		if (rootDirectory == null || rootDirectory.isEmpty()) {
			throw new HistogramServiceException("Argument Exception");
		}

		if (fileExtension == null || fileExtension.isEmpty()) {
			throw new HistogramServiceException("Argument Exception");
		}

		Path rootPath = Path.of(rootDirectory);

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
			for (Path path : stream) {
				if (Files.isDirectory(path)) {
					globalHistogram.setDirectories(globalHistogram.getDirectories() + 1);
					// recursive call of this method with subdirectory
					calculateHistogram(path.toAbsolutePath().toString(), fileExtension);
					// after finishing a directory prints result as required
					System.out.println("N:" + currentPrintedLine + " - Directory " + path.toAbsolutePath().toString() + " finished\n\t  " + HistogramHelper.printHistogram(globalHistogram));
					currentPrintedLine++;
				} else if (Files.isRegularFile(path)) {
					globalHistogram.setFiles(globalHistogram.getFiles() + 1);
					if (path.getFileName().toString().endsWith(fileExtension)) {
						globalHistogram.setProcessedFiles(globalHistogram.getProcessedFiles() + 1);
						List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
						long currentLineCount = 0;
						// initializes Array filled with 0
						long[] currentDistribution = HistogramHelper.initEmptyDistributionArray();
						for (String line : lines) {
							String cleanLine = line.toLowerCase()
									.replaceAll("\\s", "");
							char[] lineChars = cleanLine.toCharArray();
							for (char lineChar : lineChars) {
								int charIndex = HistogramHelper.characterArrayIndex(lineChar);
								if (charIndex >= 0) {
									currentDistribution[charIndex] += 1;
								}
							}
							currentLineCount += 1;

						}
						// updates global histogram
						globalHistogram.setDistribution(HistogramHelper.mergeDistributionArray(currentDistribution, globalHistogram.getDistribution()));
						globalHistogram.setLines(globalHistogram.getLines() + currentLineCount);

						// prints after every processed file
						System.out.println("N:" + currentPrintedLine + " - File " + path.toAbsolutePath().toString() + " finished !");
						currentPrintedLine++;
					}
				}
			}
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
		} catch (IOException e) {
			throw new HistogramServiceException("Error by creating SequentiaHistogram!");
		} catch (InterruptedException e) {
			throw new HistogramServiceException("Histogram Service interrupted . . .");
		}

		return globalHistogram;
	}

	@Override
	public String toString() {
		return "SequentialHistogramService";
	}

}
