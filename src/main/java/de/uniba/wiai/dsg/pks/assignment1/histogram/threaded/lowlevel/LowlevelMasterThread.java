package de.uniba.wiai.dsg.pks.assignment1.histogram.threaded.lowlevel;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

@ThreadSafe
public class LowlevelMasterThread extends Thread {

    private final String fileExtension;

    @GuardedBy(value = "this")
    private final Histogram totalHistogram;

    private final int maxThreads;
    private final Queue<Path> pathsQueue = new LinkedList<>();
    boolean running;
    private Path rootPath;

    @GuardedBy(value = "this")
    private int currentThreadCount;

    private LowlevelOutputThread lowlevelOutputThread;

    public LowlevelMasterThread(String name, String rootPath, String fileExtension, double blockingCoefficient) {
        super(name);
        this.rootPath = Path.of(rootPath);
        this.fileExtension = fileExtension;
        this.currentThreadCount = 0;
        this.totalHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(), 0, 0, 0, 0);
        // calculation for max Threads
        this.maxThreads = (int) (Runtime.getRuntime().availableProcessors() / (1 - blockingCoefficient));
        this.running = true;
    }

    @Override
    public void run() {
        // initiated so root folder gets processed to (almost same process as in while loop)
        this.lowlevelOutputThread = new LowlevelOutputThread("OutputThread");
        this.lowlevelOutputThread.start();

        totalHistogram.setDirectories(totalHistogram.getDirectories() + 1);
        LowlevelDirectoryThread lowlevelDirectoryThread = new LowlevelDirectoryThread("LowlevelDirectoryThread " +
                currentThreadCount, rootPath.toAbsolutePath().toString(), fileExtension, this, lowlevelOutputThread);
        lowlevelDirectoryThread.start();
        incrementCurrentThreadCount();
        while (running) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
                for (Path path : stream) {
                    if (Files.isDirectory(path)) {
                        totalHistogram.setDirectories(totalHistogram.getDirectories() + 1);
                        // folder gets added to pathQueue, which will be popped after processing current directory
                        pathsQueue.add(path);
                        // do nothing while more than maxThreads are running
                        while (maxThreads <= this.currentThreadCount) {
                        }
                        lowlevelDirectoryThread = new LowlevelDirectoryThread("LowlevelDirectoryThread " +
                                currentThreadCount, path.toAbsolutePath().toString(), fileExtension, this, lowlevelOutputThread);
                        lowlevelDirectoryThread.start();
                        incrementCurrentThreadCount();
                    }
                }
                if (pathsQueue.size() > 0) {
                    rootPath = pathsQueue.poll();
                } else {
                    // While loop to make sure that each thread is completed.
                    while (currentThreadCount != 0 && !lowlevelOutputThread.getPrintBool()) {
                    }
                    lowlevelOutputThread.shutdown();
                    this.running = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * reduces ThreadCount, so further Threads can be processed
     *
     * @param subHistogram Data from directoryThread, after finishing its directory
     */
    public synchronized void receiveData(Path path, Histogram subHistogram) {
        decrementCurrentThreadCount();
        this.totalHistogram.setFiles(this.totalHistogram.getFiles() + subHistogram.getFiles());
        this.totalHistogram.setProcessedFiles(this.totalHistogram.getProcessedFiles() + subHistogram.getProcessedFiles());
        this.totalHistogram.setLines(this.totalHistogram.getLines() + subHistogram.getLines());
        this.totalHistogram.setDistribution(HistogramHelper.mergeDistributionArray(subHistogram.getDistribution(), this.totalHistogram.getDistribution()));

        this.lowlevelOutputThread.receiveHistogram(path, this.totalHistogram);
    }

    public Histogram getHistogram() {
        return this.totalHistogram;
    }

    public synchronized void incrementCurrentThreadCount(){
        this.currentThreadCount++;
    }

    public synchronized void decrementCurrentThreadCount(){
        this.currentThreadCount--;
    }

    public void shutdown() {
        lowlevelOutputThread.shutdown();
        this.running = false;
    }
}
