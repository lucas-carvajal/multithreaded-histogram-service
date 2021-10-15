package de.uniba.wiai.dsg.pks.assignment2.histogram.threaded.executor;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;

import java.util.concurrent.BlockingQueue;

@NotThreadSafe
public class ExecutorOutputRunnable implements Runnable {

    @GuardedBy(value = "blockingQueue")
    private final BlockingQueue<String> blockingQueue;

    private int currentPrintedLine = 1;
    private volatile boolean finished = false;

    public ExecutorOutputRunnable(BlockingQueue<String> blockingMessagesQueue) {
        this.blockingQueue = blockingMessagesQueue;
    }

    @Override
    public void run() {
        while (!finished) {
            try {
                String entry = blockingQueue.take();

                if ("DONE".equals(entry)) {
                    finished = true;
                } else {
                    System.out.println("N:" + currentPrintedLine + " - " + entry);
                    currentPrintedLine ++;
                }
                if(Thread.currentThread().isInterrupted()){
                    throw new InterruptedException();
                }
            } catch (InterruptedException e) {
                finished = true;
            }
        }
    }
    public void shutDown(){
        this.finished = true;
    }
}
