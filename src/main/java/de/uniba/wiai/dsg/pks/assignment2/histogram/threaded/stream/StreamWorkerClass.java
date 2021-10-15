package de.uniba.wiai.dsg.pks.assignment2.histogram.threaded.stream;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NotThreadSafe
public class StreamWorkerClass {

    @GuardedBy(value = "itself")
    private final int DO_NOT_ENTER_SUBDIRECTORIES = 1;

    @GuardedBy(value = "itself")
    private final String rootPath;

    @GuardedBy(value = "itself")
    private final String fileExtension;

    @GuardedBy(value = "itself")
    private final BlockingQueue<String> blockingMessagesQueue;

    private final Thread context;

    public StreamWorkerClass(String rootPath, String fileEnding, BlockingQueue<String> blockingMessagesQueue, Thread thread){
        this.rootPath = rootPath;
        this.fileExtension = fileEnding;
        this.blockingMessagesQueue = blockingMessagesQueue;
        this.context = thread;
    }

    public Histogram generateHistogram() throws HistogramServiceException, RuntimeException {
        final List<Path> allPaths = traverseFiles();
        Histogram mainHistogram = processDirectories(allPaths);
        return mainHistogram;
    }

    private List<Path> traverseFiles() throws HistogramServiceException {
        Path folder = Paths.get(rootPath);
        List<Path> pathList;

        //walk through directory and make list of all directory paths
        try(Stream<Path> streamOfPaths = Files.walk(folder).parallel()){
            pathList = streamOfPaths
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new HistogramServiceException("IO Exception occurred: " + e);
        }

        return pathList;
    }

    private Histogram processDirectories(List<Path> pathList) throws RuntimeException{
        Optional<Histogram> returnHistogram = pathList.stream().parallel()
                .map(createDirectoryHistograms)
                .reduce((histogram1, histogram2) -> sumUpAllHistograms(histogram1, histogram2));

        if(returnHistogram.isPresent()){
            return returnHistogram.get();
        } else {
            return new Histogram(HistogramHelper.initEmptyDistributionArray(), 0, 0, 0, 0);
        }
    }

    private Function<Path, Histogram> createDirectoryHistograms = this::createDirectoryHistogramsHelper;

    private Histogram createDirectoryHistogramsHelper(Path path) throws RuntimeException{
        Histogram returnHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(), 0, 0,0 ,0);
        try (Stream<Path> streamOfPaths = Files.walk(path, DO_NOT_ENTER_SUBDIRECTORIES).parallel()) {
            Optional<Histogram> producedHistogram = streamOfPaths
                        .map(createFileHistogram)
                        .reduce((histogram1, histogram2) -> sumUpAllHistograms(histogram1, histogram2));
            if(producedHistogram.isPresent()){
                returnHistogram = producedHistogram.get();
            }
        } catch (IOException e){
            throw new RuntimeException("IO Exception occurred: " + e);
        }

        returnHistogram.setDirectories(1);

        if (context.isInterrupted()){
            throw new RuntimeException("Stream Histogram Service Interrupted");
        }

        try {
            blockingMessagesQueue.put("Directory " + path.toAbsolutePath().toString() + " finished \n\t  " + HistogramHelper.printHistogram(returnHistogram));
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted Exception occurred: " + e);
        }

        return returnHistogram;
    }

    private Function<Path, Histogram> createFileHistogram = this::createFileHistogramHelper;

    private Histogram createFileHistogramHelper(Path path) throws RuntimeException {
        Histogram fileHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(), 0, 0, 0, 0);
        if (context.isInterrupted()){
            throw new RuntimeException("Stream Histogram Service Interrupted");
        }
        if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(fileExtension)) {
            fileHistogram.setFiles(fileHistogram.getFiles() + 1);
            fileHistogram.setProcessedFiles(fileHistogram.getProcessedFiles() + 1);

            //Lines
            try(Stream<String> streamOfLines = Files.lines(path, StandardCharsets.UTF_8).parallel()) {
                    long lineCount = streamOfLines.count();
                    fileHistogram.setLines(lineCount);
            } catch (IOException e) {
                throw new RuntimeException("IO Exception occurred: " + e);
            }

            //Distribution
            try (Stream<String> streamOfLines = Files.lines(path, StandardCharsets.UTF_8).parallel()){
                Optional<long[]> distribution = streamOfLines
                        .map(mapToDistribution)
                        .reduce(HistogramHelper::mergeDistributionArray);
                if (distribution.isPresent()) {
                    fileHistogram.setDistribution(distribution.get());
                }

                blockingMessagesQueue.put("File " + path + " finished!");
            } catch (IOException e) {
                throw new RuntimeException("IO Exception occurred: " + e);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted Exception occurred: " + e);
            }

        } else if (Files.isRegularFile(path)){
            fileHistogram.setFiles(fileHistogram.getFiles() + 1);
        }

        if (context.isInterrupted()){
            throw new RuntimeException("Stream Histogram Service Interrupted");
        }

        return fileHistogram;
    }

    private Function<String, long[]> mapToDistribution = (line) ->
            mapToDistributionHelper(line);

    private long[] mapToDistributionHelper(String line) throws RuntimeException{
        long[] currentDistribution = HistogramHelper.initEmptyDistributionArray();
        String cleanLine = line.toLowerCase().replaceAll("\\s", "");
        char[] lineChars = cleanLine.toCharArray();
        for (char lineChar : lineChars) {
            int charIndex = HistogramHelper.characterArrayIndex(lineChar);
            if (charIndex >= 0) {
                currentDistribution[charIndex] += 1;
            }
        }

        if (context.isInterrupted()){
            throw new RuntimeException("Stream Histogram Service Interrupted");
        }

        return currentDistribution;
    }

    private Histogram sumUpAllHistograms(Histogram mainHistogram, Histogram secondHistogram){
        // update mainHistogram
        HistogramHelper.updateMainHistogram(mainHistogram, secondHistogram);
        return mainHistogram;
    }
}
