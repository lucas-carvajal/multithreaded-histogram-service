package de.uniba.wiai.dsg.pks.assignment2.histogram.threaded.forkjoin;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RecursiveTask;
@NotThreadSafe
public class ForkJoinDirectoryTask extends RecursiveTask<Histogram> {

  private final Histogram localHistogram;
  private final String rootDirectory;
  private final String fileExtension;
  @GuardedBy(value="blockingQueue")
  private final BlockingQueue<String> blockingQueue;

  public ForkJoinDirectoryTask(
    String rootDirectory,
    String fileExtension,
    BlockingQueue<String> blockingQueue
  ) {
    this.rootDirectory = rootDirectory;
    this.fileExtension = fileExtension;
    this.localHistogram =
      new Histogram(HistogramHelper.initEmptyDistributionArray(), 0, 0, 0, 0);
    this.blockingQueue = blockingQueue;
  }

  /**
   * Computes recursively histograms for directories
   * @return localhistogram
   */
  @Override
  protected Histogram compute() {
    List<ForkJoinDirectoryTask> forkJoinDirectoryTasks = new ArrayList<>();
    Path rootPath = Path.of(rootDirectory);
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
      for (Path path : stream) {
        if (Files.isDirectory(path)) {
          localHistogram.setDirectories(localHistogram.getDirectories() + 1);
          ForkJoinDirectoryTask task = new ForkJoinDirectoryTask(
            path.toString(),
            fileExtension,
            blockingQueue
          );
          forkJoinDirectoryTasks.add(task);
        } else if (Files.isRegularFile(path)) {
          handleFile(path);
        }
      }
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException();
      }
    } catch (IOException | InterruptedException e) {
      return null;
    }

    invokeAll(forkJoinDirectoryTasks);
    for (ForkJoinDirectoryTask tmpTask : forkJoinDirectoryTasks) {
      localHistogram.setDirectories(
        localHistogram.getDirectories() + tmpTask.join().getDirectories()
      );
      localHistogram.setFiles(
        localHistogram.getFiles() + tmpTask.join().getFiles()
      );
      localHistogram.setProcessedFiles(
        localHistogram.getProcessedFiles() + tmpTask.join().getProcessedFiles()
      );
      localHistogram.setLines(
        localHistogram.getLines() + tmpTask.join().getLines()
      );
      localHistogram.setDistribution(
        HistogramHelper.mergeDistributionArray(
          tmpTask.join().getDistribution(),
          localHistogram.getDistribution()
        )
      );
    }
    blockingQueue.add(" Directory: \t" + rootPath + " finished! \n" + HistogramHelper.printHistogram(localHistogram) );
    return localHistogram;
  }

  private void handleFile(Path path) throws IOException, InterruptedException {
    long currentLineCount = 0;
    long[] currentDistribution = HistogramHelper.initEmptyDistributionArray();

    localHistogram.setFiles(localHistogram.getFiles() + 1);
    if (path.getFileName().toString().endsWith(fileExtension)) {
      localHistogram.setProcessedFiles(localHistogram.getProcessedFiles() + 1);
      List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
      // initializes Array filled with 0
      for (String line : lines) {
        String cleanLine = line.toLowerCase().replaceAll("\\s", "");
        char[] lineChars = cleanLine.toCharArray();
        for (char lineChar : lineChars) {
          int charIndex = HistogramHelper.characterArrayIndex(lineChar);
          if (charIndex >= 0) {
            currentDistribution[charIndex] += 1;
          }
        }
        currentLineCount += 1;
      }
    }
    localHistogram.setDistribution(
      HistogramHelper.mergeDistributionArray(
        currentDistribution,
        localHistogram.getDistribution()
      )
    );
    localHistogram.setLines(localHistogram.getLines() + currentLineCount);
    blockingQueue.put(" " + path.toString() + " finished!" );
  }
}
