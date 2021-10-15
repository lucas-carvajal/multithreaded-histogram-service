package de.uniba.wiai.dsg.pks.assignment1.histogram.threaded.lowlevel;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.util.LinkedList;
import java.util.Queue;
import java.nio.file.Path;

@ThreadSafe
public class LowlevelOutputThread extends Thread {

    @GuardedBy(value = "this")
    private int currentPrintedLine;

    @GuardedBy(value = "this")
    private long fileCount;

    private boolean running;

    @GuardedBy(value ="this")
    private final Queue<String> messageQueue;

    private volatile boolean printBool;

    public LowlevelOutputThread(String name) {
        super(name);
        this.currentPrintedLine = 0;
        this.fileCount = 0;
        this.running = true;
        this.messageQueue = new LinkedList<>();
        this.printBool = false;
    }

    /**
     printbool for prioritizing PrintInfo operation, otherwise Queue is getting to full, no Blockingmessagequeue allowed :.(
     */
    @Override
    public void run() {
        while (running){
            if(this.messageQueue.size() > 0){
                printBool = true;
            }
            printInfo();
        }
    }

    /**
     * prints currentline (which is incremented) and which file has been processed
     *
     */
    public boolean getPrintBool(){
        return printBool;
    }

    /**
     * prints message from Queue
     */
    public synchronized void printInfo(){
        while(this.messageQueue.size() > 0) {
            currentPrintedLine++;
            String message = messageQueue.poll();
            System.out.println("N:" + currentPrintedLine + " - " + message);
        }
        printBool = false;
        this.notify();
    }

    /**
     *
     * @param message message from DirectoryThread
     */
    public synchronized void receiveFileInfo(String message)  {
        if(printBool){
            try {
                this.wait();
            } catch (InterruptedException e) {
                shutdown();
                System.out.println("OutputThread interrupted . . . ");
            }
        }
        this.messageQueue.add(message);
        this.notify();
    }

    /**
     * ends while loop in run()
     */
    public void shutdown() {
        this.running = false;
    }

    /**
     * After finishing one folder the Masterthreads sends total Histogram to this method.
     * Method prints Histogram, but only if new files have been processed.
     *
     * @param newHistogram Histogram from Masterthread
     */
    public synchronized void receiveHistogram(Path path, Histogram newHistogram) {
            fileCount = newHistogram.getFiles();
            String message = HistogramHelper.printHistogram(newHistogram);
            this.messageQueue.add("Directory " + path.toAbsolutePath().toString() + " finished \n\t  " + message);
            this.notify();
    }
}
