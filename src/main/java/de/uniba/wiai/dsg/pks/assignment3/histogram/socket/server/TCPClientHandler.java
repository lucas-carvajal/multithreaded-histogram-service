package de.uniba.wiai.dsg.pks.assignment3.histogram.socket.server;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.GetResult;
import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.ParseDirectory;
import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.ReturnResult;
import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.TerminateConnection;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.*;

@ThreadSafe
public class TCPClientHandler implements ClientHandler {
	private final Socket clientSocket;
	private final TCPDirectoryServer server;
	private final Histogram clientHistogram;
	private boolean running;
	private final ExecutorService exec;
	private Phaser phaser;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;

	public TCPClientHandler(Socket client, TCPDirectoryServer server) {
		this.clientSocket = client;
		this.server = server;
		this.running = true;
		this.exec = Executors.newCachedThreadPool();
		this.clientHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(),0,0,0,1);
	}

	/**
	 * Handles messages from client. First initializes Phaser (as StopLatch) and afterwards waits for messages. Depending on the ObjectType of
	 * the message creates a new Runnable. Specific runnable (DirectoryRunnable) needed for GetResult.
	 */
	@Override
	public void run() {
		phaser = new Phaser();
		while (running) {
			try {
				inputStream = new ObjectInputStream(clientSocket.getInputStream());
				Object object = inputStream.readObject();
				if (object instanceof ParseDirectory) {
					ParseDirectory parseDirectory = (ParseDirectory) object;
					exec.submit(new DirectoryRunnable(phaser,this,parseDirectory));
				} else if (object instanceof TerminateConnection) {
					this.running = false;
					TerminateConnection terminateConnection = (TerminateConnection) object;
					exec.submit(() -> process(terminateConnection));
				}
				else {
					GetResult getResult = (GetResult) object;
					exec.submit(() -> process(getResult));
				}
			} catch (IOException e) {
				System.err.println("Error by getting message from Client " + e.getMessage());
				this.running = false;
			} catch (ClassNotFoundException e) {
				System.err.println("Error by reading message from Client! Can't identify class of object " + e.getMessage());
			}
		}
	}


	/**
	 * Processes directory and writes result into clientHistogram
	 * @param parseDirectory directory to be processed
	 */
	@Override
	public void process(ParseDirectory parseDirectory) {
		Histogram localHistogram;
		Optional<Histogram> histogram = this.server.getCachedResult(parseDirectory);
		if (histogram.isEmpty()) {
			localHistogram = new Histogram(HistogramHelper.initEmptyDistributionArray(),0,0,0,0);
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(parseDirectory.getPath()))) {
				for (Path path : stream) {
					if (Files.isDirectory(path)) {
						localHistogram.setDirectories(localHistogram.getDirectories()+1);
					}
					else if (Files.isRegularFile(path)){
						localHistogram.setFiles(localHistogram.getFiles() + 1);
						if (path.getFileName().toString().endsWith(parseDirectory.getFileExtension())) {
							HistogramHelper.processFile(path, localHistogram);
						}
					}
				}
				server.putInCache(parseDirectory,localHistogram);
				updateHistogram(localHistogram);
			} catch (IOException e) {
				System.err.println("Something wrong with the path of parseDirectory " + e.getMessage());
			}
		} else {
			localHistogram = histogram.get();
			updateHistogram(localHistogram);
		}
	}

	/**
	 * Waits via phaser for all DirectoryRunnables to finish. Afterwards sends clientHistogram (result) to client
	 * @param getResult message Object from client
	 * @return Message sent to the client
	 */
	@Override
	public ReturnResult process(GetResult getResult) {
		phaser.awaitAdvance(0);

		ReturnResult returnResult = new ReturnResult(clientHistogram);

		try {
			outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
			outputStream.writeObject(returnResult);
			outputStream.flush();
		} catch (IOException e) {
			System.err.println("Error by sending message to Client " + e.getMessage());
		}
		return returnResult;
	}

	/**
	 * Closes connection to client, shuts down ExecutorService and forwards disconnect information to TCPDirectoryServer
	 * @param terminateConnection message Object from client
	 */
	@Override
	public void process(TerminateConnection terminateConnection) {
		System.out.print("\nTERMINATED");
		running = false;
		try {
			inputStream.close();
			outputStream.close();
			this.clientSocket.close();

		} catch (IOException e) {
			System.err.println("Error by closing Socket " + e.getMessage());
		} finally {
			HistogramHelper.shutdownAndAwaitTermination(exec);
			server.disconnect(this);
		}
	}

	/**
	 * Updates clientHistogram
	 * @param localHistogram histogram with which clientHistogram should be updated
	 */
	public synchronized void updateHistogram(Histogram localHistogram) {
		clientHistogram.setDirectories(localHistogram.getDirectories() + clientHistogram.getDirectories());
		clientHistogram.setFiles(localHistogram.getFiles() + clientHistogram.getFiles());
		clientHistogram.setProcessedFiles(localHistogram.getProcessedFiles() + clientHistogram.getProcessedFiles());
		clientHistogram.setLines(localHistogram.getLines() + clientHistogram.getLines());
		clientHistogram.setDistribution(HistogramHelper.mergeDistributionArray(localHistogram.getDistribution(), clientHistogram.getDistribution()));
	}
}
