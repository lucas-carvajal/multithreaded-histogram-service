package de.uniba.wiai.dsg.pks.assignment3.histogram.socket.server;

import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

	public static void main(String[] args) throws DirectoryServerException,
			IOException {
		ExecutorService exec = Executors.newFixedThreadPool(1);
		TCPDirectoryServer server = new TCPDirectoryServer(1337);
		exec.submit(server);

		System.out.println("Server started. Press enter to terminate.");

		System.in.read();

		server.shutdown();
		HistogramHelper.shutdownAndAwaitTermination(exec);
		System.out.println("\nServer is shut down...");

	}
}
