package de.uniba.wiai.dsg.pks.assignment.shared;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class HistogramHelper {
    private final static char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    // merges two Arrays
    public static long[] mergeDistributionArray(long[] subArray, long[] returnArray) {
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = subArray[i] + returnArray[i];
        }
        return returnArray;
    }

    // initializes an  distributionarray filled with 0
    public static long[] initEmptyDistributionArray() {
        long[] alphabet = new long[Histogram.ALPHABET_SIZE];
        Arrays.fill(alphabet, 0);
        return alphabet;
    }

    // returns the arraylocation of a letter
    public static int characterArrayIndex(char c) {
        for (int i = 0; i < alphabet.length; i++) {
            if (c == alphabet[i]) {
                return i;
            }
        }

        return -1;
    }

    public static void processFile(Path path, Histogram localHistogram) {
        try {
            localHistogram.setProcessedFiles(localHistogram.getProcessedFiles() + 1);
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            long currentLineCount = 0;
            long[] currentDistribution = localHistogram.getDistribution();
            // iterate over all lines in file
            for(String line : lines){
                String cleanLine = line.toLowerCase().replaceAll("\\s","");
                char[] lineChars = cleanLine.toCharArray();
                // process each line in the file
                for(char lineChar: lineChars){
                    int charIndex = HistogramHelper.characterArrayIndex(lineChar);
                    if(charIndex >= 0){
                        currentDistribution[charIndex] += 1;
                        localHistogram.setDistribution(currentDistribution);
                    }
                }
                currentLineCount += 1;
            }

            localHistogram.setLines(localHistogram.getLines() + currentLineCount);
        } catch (IOException e) {
            System.err.println("Error be reading lines of ProceedFile");
        }
    }

    // prints Histogram as required from assignmentdescription
    public static String printHistogram(Histogram histogram) {
        StringBuilder distr = new StringBuilder();
        for (long aChar : histogram.getDistribution()) {
            distr.append(aChar).append(",");
        }
        return "[distr = [" + distr + "],\n\t  "
                + "lines = " + histogram.getLines()
                + ", files = " + histogram.getFiles()
                + ", processedFiles = " + histogram.getProcessedFiles()
                + ", directories = " + histogram.getDirectories()
                + "]";
    }

    // terminate Threadpool
    public static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public static void updateMainHistogram(Histogram mainHistogram, Histogram localHistogram) {
        mainHistogram.setDistribution(HistogramHelper.mergeDistributionArray(localHistogram.getDistribution(), mainHistogram.getDistribution()));
        mainHistogram.setLines(mainHistogram.getLines() + localHistogram.getLines());
        mainHistogram.setFiles(mainHistogram.getFiles() + localHistogram.getFiles());
        mainHistogram.setProcessedFiles(mainHistogram.getProcessedFiles() + localHistogram.getProcessedFiles());
        mainHistogram.setDirectories(mainHistogram.getDirectories() + localHistogram.getDirectories());
    }

}
