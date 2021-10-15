package de.uniba.wiai.dsg.pks.assignment1.histogram.threaded.lowlevel;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@ThreadSafe
public class LowlevelDirectoryThread extends Thread {

    private final String dirPath;
    private final String fileExtension;
    private final Histogram localHistogram;
    private final LowlevelMasterThread lowlevelMasterThread;
    private final LowlevelOutputThread outputThread;

    public LowlevelDirectoryThread(String name, String dirPath, String fileExtension, LowlevelMasterThread lowlevelMasterThread, LowlevelOutputThread outputThread) {
        super(name);
        this.dirPath = dirPath;
        this.fileExtension = fileExtension;
        this.localHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(), 0, 0, 0, 0);
        this.lowlevelMasterThread = lowlevelMasterThread;
        this.outputThread = outputThread;
    }

    @Override
    public void run() {
        Path rootPath = Path.of(dirPath);
        // similiar to Sequential implementation
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    localHistogram.setFiles(localHistogram.getFiles() + 1);
                    if (path.getFileName().toString().endsWith(fileExtension)) {
                        localHistogram.setProcessedFiles(localHistogram.getProcessedFiles() + 1);

                        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                        long currentLineCount = 0;
                        long[] currentDistribution = HistogramHelper.initEmptyDistributionArray();
                        for (String line : lines) {
                            String cleanLine = line.toLowerCase()
                                    .replaceAll("\\s", "");
                            char[] lineChars = cleanLine.toCharArray();
                            for (char lineChar : lineChars) {
                                int charIndex = HistogramHelper.characterArrayIndex(lineChar);
                                if (charIndex >= 0) {
                                    currentDistribution[charIndex] += 1;
                                }
                            }
                            currentLineCount += 1;
                        }
                        localHistogram.setDistribution(HistogramHelper.mergeDistributionArray(currentDistribution, localHistogram.getDistribution()));
                        localHistogram.setLines(localHistogram.getLines() + currentLineCount);

                        outputThread.receiveFileInfo(path.toAbsolutePath().toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // sends subhistogram from this directory to masterthread
        lowlevelMasterThread.receiveData(rootPath, localHistogram);
    }
}