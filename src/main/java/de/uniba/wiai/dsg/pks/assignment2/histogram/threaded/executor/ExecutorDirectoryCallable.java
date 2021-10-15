package de.uniba.wiai.dsg.pks.assignment2.histogram.threaded.executor;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

@ThreadSafe
public class ExecutorDirectoryCallable implements Callable<Histogram> {

    private final Path dirPath;

    @GuardedBy(value = "itself")
    private final String fileExtension;
    
    private Histogram localHistogram;

    @GuardedBy(value = "itself")
    private final BlockingQueue<String> blockingQueue;

    public ExecutorDirectoryCallable(Path dirPath, String fileExtension, BlockingQueue<String> blockingQueue) {
        this.dirPath = dirPath;
        this.fileExtension = fileExtension;
        this.blockingQueue = blockingQueue;
        this.localHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(),0,0,0,0);
    }

    @Override
    public Histogram call() throws InterruptedException, HistogramServiceException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            // iterate through the directory
            for (Path path : stream) {
                if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(fileExtension)) {
                    // go through this file
                    HistogramHelper.processFile(path, localHistogram);
                    putProcessedFilesIntoBlockingQueue(String.valueOf(path));
                    localHistogram.setFiles(localHistogram.getFiles() + 1);
                } else if (Files.isRegularFile(path)) {
                    localHistogram.setFiles(localHistogram.getFiles() + 1);
                }
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Exception");
            }
        } catch (IOException e) {
            throw new HistogramServiceException("Error by iterating Directory!");
        }
        localHistogram.setDirectories(1);

        blockingQueue.put("Directory " + dirPath.toAbsolutePath().toString() + " finished \n\t  " + HistogramHelper.printHistogram(localHistogram));

        return localHistogram;
    }

    private void putProcessedFilesIntoBlockingQueue(String name) throws InterruptedException {
        blockingQueue.put("File " + name + " finished!");
    }
}
