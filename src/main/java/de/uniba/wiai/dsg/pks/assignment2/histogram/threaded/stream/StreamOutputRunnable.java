package de.uniba.wiai.dsg.pks.assignment2.histogram.threaded.stream;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;

import java.util.concurrent.BlockingQueue;

@NotThreadSafe
public class StreamOutputRunnable implements Runnable {

    @GuardedBy(value = "itself")
    private final BlockingQueue<String> blockingQueue;

    private int currentPrintedLine = 1;

    public StreamOutputRunnable(BlockingQueue<String> blockingMessagesQueue) {
        this.blockingQueue = blockingMessagesQueue;
    }

    @Override
    public void run() {
        boolean finished = false;
        while (!finished) {

            try {
                String entry = blockingQueue.take();

                if ("DONE".equals(entry)) {
                    finished = true;;
                } else {
                    System.out.println("N:" + currentPrintedLine + " - " + entry);
                    currentPrintedLine ++;
                }
            } catch (InterruptedException e) {
                finished = true;
            }
        }
    }
}
