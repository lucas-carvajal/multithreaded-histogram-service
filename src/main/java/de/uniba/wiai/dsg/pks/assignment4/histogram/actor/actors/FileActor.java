package de.uniba.wiai.dsg.pks.assignment4.histogram.actor.actors;

import akka.actor.*;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.FileRequestMessage;
import de.uniba.wiai.dsg.pks.assignment4.histogram.actor.messages.FileResponseMessage;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@NotThreadSafe
public class FileActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(FileRequestMessage.class, this::process)
                .matchAny(any -> {
                    throw new ClassNotFoundException("Received unknown message: " + any);
                })
                .build();
    }

    private void process(FileRequestMessage fileRequestMessage) throws IOException {
            List<String> lines = Files.readAllLines(Path.of(fileRequestMessage.getPath()), StandardCharsets.UTF_8);
            int currentLineCount = 0;
            long[] currentDistribution = HistogramHelper.initEmptyDistributionArray();
            // iterate over all lines in file
            for(String line : lines){
                String cleanLine = line.toLowerCase().replaceAll("\\s","");
                char[] lineChars = cleanLine.toCharArray();
                // process each line in the file
                for(char lineChar: lineChars){
                    int charIndex = HistogramHelper.characterArrayIndex(lineChar);
                    if(charIndex >= 0){
                        currentDistribution[charIndex] += 1;
                    }
                }
                currentLineCount += 1;
            }

            FileResponseMessage fileResponseMessage = new FileResponseMessage(Arrays.stream(currentDistribution).boxed().collect(Collectors.toList()),
                    fileRequestMessage.getPath(),currentLineCount);
            getSender().tell(fileResponseMessage,ActorRef.noSender());
    }

}
