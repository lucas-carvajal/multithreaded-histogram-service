package de.uniba.wiai.dsg.pks.assignment2.histogram.threaded.forkjoin;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramService;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.*;

@ThreadSafe
public class ForkJoinHistogramService implements HistogramService {


  public ForkJoinHistogramService() {
    // REQUIRED FOR GRADING - DO NOT REMOVE DEFAULT CONSTRUCTOR
    // but you can add code below
  }

  @Override
  public Histogram calculateHistogram(
    String rootDirectory,
    String fileExtension
  ) throws HistogramServiceException {

    if (rootDirectory == null || rootDirectory.isEmpty()) {
      throw new HistogramServiceException("Argument Exception");
    }

    if (fileExtension == null || fileExtension.isEmpty()) {
      throw new HistogramServiceException("Argument Exception");

    }

    Histogram mainHistogram;

    BlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(5000, true);
    ForkJoinPool forkJoinPool = new ForkJoinPool();
    ForkJoinPool forkJoinOutputPool = new ForkJoinPool();

    ForkJoinOutputRunnable outputRunnable = new ForkJoinOutputRunnable(
            blockingQueue
    );
    forkJoinOutputPool.submit(outputRunnable);

    ForkJoinDirectoryTask forkJoinDirectoryTask = new ForkJoinDirectoryTask(
            rootDirectory,
            fileExtension,
            blockingQueue
    );
    Future<Histogram> resultFuture = forkJoinPool.submit(forkJoinDirectoryTask);
    try {
      mainHistogram = resultFuture.get();
    } catch (ExecutionException e) {
      throw new HistogramServiceException(
              "Error in ExecutionService " + e.getCause()
      );
    } catch (InterruptedException e) {
      forkJoinPool.shutdownNow();
      forkJoinOutputPool.shutdownNow();
      throw new HistogramServiceException("Histogram Service interrupted");

    } finally {
      outputRunnable.shutDown();
      HistogramHelper.shutdownAndAwaitTermination(forkJoinPool);
      HistogramHelper.shutdownAndAwaitTermination(forkJoinOutputPool);

    }
    // +1 for rootdirectory
    mainHistogram.setDirectories(mainHistogram.getDirectories() + 1);

  return mainHistogram;

  }

  @Override
  public String toString() {
    return "ForkJoinHistogramService";
  }
}
