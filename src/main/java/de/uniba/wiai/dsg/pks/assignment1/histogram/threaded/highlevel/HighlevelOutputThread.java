package de.uniba.wiai.dsg.pks.assignment1.histogram.threaded.highlevel;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.BlockingQueue;

@ThreadSafe
public class HighlevelOutputThread extends Thread {

    @GuardedBy(value = "itself")
    private final BlockingQueue<String> blockingMessagesQueue;
    private int currentPrintedLine = 1;

    public HighlevelOutputThread(BlockingQueue<String> blockingMessagesQueue) {
        this.blockingMessagesQueue = blockingMessagesQueue;
    }

    public void run() {
        boolean finished = false;
        while (!finished) {

            try {
                String entry = blockingMessagesQueue.take();

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
