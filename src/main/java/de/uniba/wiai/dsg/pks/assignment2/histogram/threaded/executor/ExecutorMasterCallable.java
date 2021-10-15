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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

@ThreadSafe
public class ExecutorMasterCallable implements Callable<Histogram> {

    @GuardedBy(value = "itself")
    private final String rootPath;

    @GuardedBy(value = "itself")
    private final String fileExtension;

    private ExecutorService outputPool;
    private ExecutorService dirPool;

    @GuardedBy(value = "itself")
    private Histogram mainHistogram;

    @GuardedBy(value = "itself")
    private final BlockingQueue<String> blockingQueue;

    private final List<Path> listOfDirPath = new LinkedList<>();

    public ExecutorMasterCallable(String rootPath, String fileExtension) {
        this.rootPath = rootPath;
        this.fileExtension = fileExtension;
        this.blockingQueue = new ArrayBlockingQueue<>(5000, true);
    }

    @Override
    public Histogram call() throws HistogramServiceException {

        ExecutorOutputRunnable outputRunnable = new ExecutorOutputRunnable(blockingQueue);
        try {
            // initialize Pool for OutputThread and DirectoryThread
            outputPool = Executors.newSingleThreadExecutor();
            dirPool = Executors.newCachedThreadPool();
            outputPool.submit(outputRunnable);

            listOfDirPath.add(Path.of(rootPath));
            getListOfDirPath(Path.of(rootPath));

            mainHistogram = getResult(listOfDirPath);
            blockingQueue.put("DONE");
        } catch (InterruptedException e) {
            outputRunnable.shutDown();
            outputPool.shutdownNow();
            dirPool.shutdownNow();
            throw new HistogramServiceException("Error by executing directories!");
        } finally {
            outputRunnable.shutDown();
            HistogramHelper.shutdownAndAwaitTermination(outputPool);
            HistogramHelper.shutdownAndAwaitTermination(dirPool);
        }

        return mainHistogram;
    }

    /**
     * Gets a list of directory's paths
     * @param rootPath path of a directory
     */
    private void getListOfDirPath(Path rootPath) throws HistogramServiceException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    listOfDirPath.add(path);
                    getListOfDirPath(path);
                }
            }
        } catch (IOException e) {
            throw new HistogramServiceException("Error by executing directories!");
        }
    }

    /**
     * Gets the main Histogram by getting results of all DirectoryThread
     *
     * @param listOfDirPath input as a list of Directory Paths
     * @return the mainHistogram for that Program
     * @throws HistogramServiceException if there are something wrong by executing result from DirectoryThread
     */
    private Histogram getResult(List<Path> listOfDirPath) throws HistogramServiceException {
        Histogram mainHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(), 0, 0, 0, 0);

        for (Path path : listOfDirPath) {
            // create directoryThread for current directory
            Future<Histogram> result = dirPool.submit(new ExecutorDirectoryCallable(path, fileExtension, blockingQueue));
            try {
                // listOfLocalResult.add(result);
                Histogram localHistogram = result.get(60, TimeUnit.SECONDS);

                // update mainHistogram
                mainHistogram.setDistribution(HistogramHelper.mergeDistributionArray(localHistogram.getDistribution(), mainHistogram.getDistribution()));
                mainHistogram.setLines(mainHistogram.getLines() + localHistogram.getLines());
                mainHistogram.setFiles(mainHistogram.getFiles() + localHistogram.getFiles());
                mainHistogram.setProcessedFiles(mainHistogram.getProcessedFiles() + localHistogram.getProcessedFiles());
                mainHistogram.setDirectories(mainHistogram.getDirectories() + 1);

            } catch (InterruptedException e) {
                outputPool.shutdownNow();
                dirPool.shutdownNow();
                throw new HistogramServiceException("Error by executing local histogram!");
            } catch (ExecutionException e) {
                HistogramHelper.shutdownAndAwaitTermination(dirPool);
                throw new HistogramServiceException("Error by executing local histogram!");
            } catch (TimeoutException e) {
                result.cancel(true);
            }
        }
        return mainHistogram;
    }
}
