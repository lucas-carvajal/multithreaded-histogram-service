package de.uniba.wiai.dsg.pks.assignment1.histogram.threaded.highlevel;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

@ThreadSafe
public class HighlevelDirectoryThread extends Thread {

    private final Path rootPath;
    private final String fileExtension;

    @GuardedBy(value = "histogramSemaphore")
    private Histogram mainHistogram;
    private Histogram localHistogram;

    @GuardedBy(value = "itself")
    private final Semaphore histogramSemaphore;

    @GuardedBy(value = "itself")
    private final BlockingQueue<String> blockingMessagesQueue;

    public HighlevelDirectoryThread(Path rootPath, String fileExtension, Histogram mainHistogram, Semaphore histogramSemaphore, BlockingQueue<String> blockingQueue) {
        this.rootPath = rootPath;
        this.fileExtension = fileExtension;
        this.mainHistogram = mainHistogram;
        this.histogramSemaphore = histogramSemaphore;
        this.blockingMessagesQueue = blockingQueue;
        localHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(),0,0,0,0);
    }

    @Override
    public void run(){
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
                //iterate through directories, recursively go into subdirectories
                for (Path path : stream) {
                    if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(fileExtension)) {
                        processFile(path);
                        printFileFinished(String.valueOf(path));
                        localHistogram.setFiles(localHistogram.getFiles() + 1);
                    } else if (Files.isRegularFile(path)) {
                        localHistogram.setFiles(localHistogram.getFiles() + 1);
                    }
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            localHistogram.setDirectories(1);

            addHistogramAndPrintDirectoryFinished();
        } catch (InterruptedException e) {

        }
    }

    private void addHistogramAndPrintDirectoryFinished() throws InterruptedException {
        // add localHistogram to mainHistogram and print out that current directory is finished

            histogramSemaphore.acquire();
            try {
                // add localHistogram to mainHistogram
                mainHistogram.setLines(mainHistogram.getLines() + localHistogram.getLines());
                mainHistogram.setFiles(mainHistogram.getFiles() + localHistogram.getFiles());
                mainHistogram.setProcessedFiles(mainHistogram.getProcessedFiles() + localHistogram.getProcessedFiles());
                mainHistogram.setDirectories(mainHistogram.getDirectories() + localHistogram.getDirectories());
                long[] distribution = mainHistogram.getDistribution();
                long[] localDistribution = localHistogram.getDistribution();
                for (int i = 0; i < localHistogram.getDistribution().length; i++) {
                    distribution[i] = localDistribution[i] + distribution[i];
                }
                mainHistogram.setDistribution(distribution);

                // print out that directory is finished and print out current mainHistogram
                blockingMessagesQueue.put("Directory " + rootPath.toAbsolutePath().toString() + " finished \n\t  " + HistogramHelper.printHistogram(mainHistogram));
            } finally {
                histogramSemaphore.release();
            }
        }

    private void processFile(Path path) {
        try {
            localHistogram.setProcessedFiles(localHistogram.getProcessedFiles() + 1);
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            long currentLineCount = 0;
            long[] currentDistribution = localHistogram.getDistribution();
            // iterate over all lines in file
            for(String line : lines){
                String cleanLine = line.toLowerCase().replaceAll("\\s","");
                char[] lineChars = cleanLine.toCharArray();
                // process each line in the file
                for(char lineChar: lineChars){
                    int charIndex = HistogramHelper.characterArrayIndex(lineChar);
                    if(charIndex >= 0){
                        currentDistribution[charIndex] += 1;
                        localHistogram.setDistribution(currentDistribution);
                    }
                }
                currentLineCount += 1;
            }
            localHistogram.setLines(localHistogram.getLines() + currentLineCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printFileFinished(String name) throws InterruptedException{
        //push message that file processing is finished to blockingQueue
            blockingMessagesQueue.put("File " + name + " finished!");
    }
}
