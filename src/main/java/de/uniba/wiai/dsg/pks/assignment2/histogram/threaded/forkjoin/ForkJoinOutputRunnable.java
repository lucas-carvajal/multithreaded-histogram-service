package de.uniba.wiai.dsg.pks.assignment2.histogram.threaded.forkjoin;

import java.util.concurrent.BlockingQueue;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class ForkJoinOutputRunnable implements Runnable {

  @GuardedBy(value = "blockingQueue")
  private final BlockingQueue<String> blockingQueue;

  private volatile boolean running = true;

  private int currentPrintedLine = 1;

  public ForkJoinOutputRunnable(BlockingQueue<String> blockingQueue) {
    this.blockingQueue = blockingQueue;
  }

  /**
   * Prints every created local Histogram from ForkJoinDirectoryTask
   */
  @Override
  public void run() {
    while (running) {
      try {
        if (!blockingQueue.isEmpty()) {
          System.out.println(
            "Nr. " + currentPrintedLine + blockingQueue.take()
          );
          currentPrintedLine++;
        }
      } catch (InterruptedException e) {
        running = false;
      }
    }
  }

  public void shutDown() {
    this.running = false;
  }
}
