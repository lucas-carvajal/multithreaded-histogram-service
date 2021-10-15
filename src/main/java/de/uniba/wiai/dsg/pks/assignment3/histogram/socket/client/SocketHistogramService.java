package de.uniba.wiai.dsg.pks.assignment3.histogram.socket.client;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramService;
import de.uniba.wiai.dsg.pks.assignment.model.HistogramServiceException;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.GetResult;
import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.ParseDirectory;
import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.TerminateConnection;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;

@NotThreadSafe
public class SocketHistogramService implements HistogramService {

	private final String hostname;
	private final int port;
	private Histogram mainHistogram;
	private final Queue<Path> pathsQueue = new LinkedList<>();
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	private Future<Histogram> callableResult;

	public SocketHistogramService(String hostname, int port) {
		// REQUIRED FOR GRADING - DO NOT CHANGE SIGNATURE
		// but you can add code below
		this.hostname = hostname;
		this.port = port;
	}

	@Override
	public Histogram calculateHistogram(String rootDirectory,
			String fileExtension) throws HistogramServiceException {

		ExecutorService executorService = Executors.newCachedThreadPool();

		// check if root directory is valid
		if (rootDirectory == null || rootDirectory.isEmpty()) {
			throw new HistogramServiceException("Root Directory must not be null or empty");
		}

		if (fileExtension == null || fileExtension.isEmpty()) {
			throw new HistogramServiceException("File Extension must not be null or empty");
		}

		Path firstPath = Path.of(rootDirectory);
		pathsQueue.add(firstPath);
		Socket server = new Socket();

		// connection setup
		try (server) {
			server.connect(new InetSocketAddress(hostname, port));
			// when the connection to Server is successful
			if(server.isConnected()){
				System.out.println("Connected to the server ...");
			}

			// send ParseDirectory request for root directory to the server
			sendParseDirectoryToServer(rootDirectory, fileExtension, server);

			while (pathsQueue.size() > 0) {
				// iterates root directory
				Path rootPath = pathsQueue.poll();

				try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootPath)) {
					// send ParseDirectory request for every directory to the server
					for (Path path : stream) {
							if (Files.isDirectory(path)) {
								pathsQueue.add(path);
								sendParseDirectoryToServer(path.toString(), fileExtension, server);
							}

						// sends TerminateConnection to Server when STOP button is clicked
						if (Thread.currentThread().isInterrupted()) {
							sendTerminateConnectionToServer(server);
							throw new HistogramServiceException("Histogram Service interrupted...");
						}
					}
				}
			}

			outputStream = new ObjectOutputStream(server.getOutputStream());
			// sends GetResult to Server
			outputStream.writeObject(new GetResult());
			outputStream.flush();
			System.out.println("A GetResult has been sent to Server ");

			inputStream = new ObjectInputStream(server.getInputStream());

			// gets mainHistogram
			callableResult = executorService.submit(new ClientListenerCallable(inputStream));
			mainHistogram = callableResult.get(5, TimeUnit.MINUTES);

			sendTerminateConnectionToServer(server);

		} catch (IOException e) {
			System.err.println("Error by connecting to Server! " + e.getMessage());
			throw new HistogramServiceException("Could not connect to Server");
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			callableResult.cancel(true);
			try {
				sendTerminateConnectionToServer(server);
			} catch (IOException ie) {
				System.err.println("Error by sending TerminateConnection to Server " + e.getMessage());
				throw new HistogramServiceException("Could not sent any message to Server");
			}
		} catch (ExecutionException e) {
			throw new HistogramServiceException("Something wrong by getting ReturnResult " + e.getMessage());
		} catch (TimeoutException e) {
			throw new HistogramServiceException("Time is out while getting ReturnResult " + e.getMessage());
		} finally {
			HistogramHelper.shutdownAndAwaitTermination(executorService);
			this.close();
		}

		return mainHistogram;
	}

	@Override
	public String toString() {
		return "SocketHistogramService";
	}

	private void sendParseDirectoryToServer(String rootDirectory, String fileExtension, Socket server) throws IOException {
		// send ParseDirectory request to server
		outputStream = new ObjectOutputStream(server.getOutputStream());
		outputStream.writeObject(new ParseDirectory(rootDirectory, fileExtension));
		outputStream.flush();
		System.out.println("A ParseDirectory has been sent to Server!");
		System.out.println("DIRECTORY: " + rootDirectory);
	}

	private void sendTerminateConnectionToServer(Socket server) throws IOException {
		try (ObjectOutputStream outputStream = new ObjectOutputStream(server.getOutputStream())) {
			outputStream.writeObject(new TerminateConnection());
			outputStream.flush();
			System.out.println("A TerminateConnection has been sent to Server...");
		}
	}

	private void close() {
		// close input and output streams
		try {
			if (inputStream != null) inputStream.close();
			if (outputStream != null) outputStream.close();
		} catch (IOException e) {
			System.err.println("Could not close Resources " + e.getMessage());
		}
	}
}
