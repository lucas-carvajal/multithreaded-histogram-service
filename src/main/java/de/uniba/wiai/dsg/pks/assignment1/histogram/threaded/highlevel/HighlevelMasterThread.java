package de.uniba.wiai.dsg.pks.assignment1.histogram.threaded.highlevel;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

@ThreadSafe
public class HighlevelMasterThread extends Thread {

    private final String rootPath;
    private final String fileExtension;

    @GuardedBy(value = "histogramSemaphore")
    private Histogram mainHistogram;

    @GuardedBy(value = "itself")
    private final Semaphore histogramSemaphore;

    @GuardedBy(value = "itself")
    private final BlockingQueue<String> blockingMessagesQueue;

    private List<HighlevelDirectoryThread> directoryThreadList;

    public HighlevelMasterThread(String rootPath, String fileExtension, Histogram histogram, Semaphore histogramSemaphore) {
        this.rootPath = rootPath;
        this.fileExtension = fileExtension;
        this.mainHistogram = histogram;
        this.histogramSemaphore = histogramSemaphore;
        blockingMessagesQueue = new ArrayBlockingQueue<String>(5000, true);
        directoryThreadList = new LinkedList<>();
    }

    @Override
    public void run() {
        // start output thread

        HighlevelOutputThread outputThread = new HighlevelOutputThread(blockingMessagesQueue);
        outputThread.start();
        try {
            // iterate over directories
            iterateDirectories(Path.of(rootPath));
            joinThreads();

            // tell output thread to terminate
            blockingMessagesQueue.put("DONE");
            outputThread.join();
        } catch (InterruptedException e) {
            outputThread.interrupt();
            for (HighlevelDirectoryThread thread : directoryThreadList){
                thread.interrupt();
            }
        }
    }

    private void joinThreads() throws InterruptedException{
        for (HighlevelDirectoryThread thread : directoryThreadList){
                thread.join();
        }
    }

    public void iterateDirectories(Path rootPath) throws InterruptedException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
            // iterate through directories, recursively go into subdirectories
            if (currentThread().isInterrupted()){
                throw new InterruptedException();
            }
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    iterateDirectories(path);
                }
            }

            // create directoryThread for current directory
            HighlevelDirectoryThread directoryThread = new HighlevelDirectoryThread(rootPath, fileExtension, mainHistogram, histogramSemaphore, blockingMessagesQueue);
            directoryThreadList.add(directoryThread);
            directoryThread.start();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
