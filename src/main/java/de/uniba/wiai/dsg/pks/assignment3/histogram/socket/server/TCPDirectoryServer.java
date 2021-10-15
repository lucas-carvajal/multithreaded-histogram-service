package de.uniba.wiai.dsg.pks.assignment3.histogram.socket.server;

import de.uniba.wiai.dsg.pks.assignment.model.Histogram;
import de.uniba.wiai.dsg.pks.assignment.shared.HistogramHelper;
import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.ParseDirectory;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@NotThreadSafe
public class TCPDirectoryServer implements DirectoryServer {
	private boolean running;
	private Cache cache;
	private final List<ClientHandler> clients = new LinkedList<>();
	private final ExecutorService exec;
	private final int runningPort;

	public TCPDirectoryServer(int port) {
		this.runningPort = port;
		this.exec = Executors.newCachedThreadPool();
	}

	/**
	 * Handles client requests and forwards to clientHandler
	 * @param port which port the directory server shall start
	 * @throws DirectoryServerException if connection can't be created
	 */

	@Override
	public void start(int port) throws DirectoryServerException {
		this.cache = new ServerCache();
		running = true;
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			serverSocket.setSoTimeout(10000);
			while (running) {
				try {
					Socket client = serverSocket.accept();
					ClientHandler clientHandler = connect(client);
					clients.add(clientHandler);
					exec.submit(clientHandler);
				}catch (SocketTimeoutException e){
					if (Thread.currentThread().isInterrupted()){
						throw new InterruptedException();
					}
				}
			}
		} catch (IOException e){
			throw new DirectoryServerException("Could not create new Connection");
		} catch (InterruptedException e)  {
			System.out.println("\n initializing shutdown");
		}
		finally
		{
			shutdown();
		}

	}

	@Override
	public void disconnect(ClientHandler clientHandler) {
		this.clients.remove(clientHandler);
		System.out.print("\nOne client disconnected. " + clients.size() + " remaining clients");
	}

	@Override
	public void shutdown() throws DirectoryServerException {
		System.out.print("\nShutting down TCPDirectoryServer");
		this.running = false;
		HistogramHelper.shutdownAndAwaitTermination(exec);
	}

	@Override
	public void run() {
		System.out.println("\nTCPDirectoryServer has been started");
		try {
			this.start(runningPort);
		} catch (DirectoryServerException e) {
			System.err.println("The following error occurred: " + e.getMessage());
			HistogramHelper.shutdownAndAwaitTermination(exec);
		}
	}

	@Override
	public Optional<Histogram> getCachedResult(ParseDirectory request) {
		return cache.getCachedResult(request);
	}

	@Override
	public void putInCache(ParseDirectory request, Histogram result) {
		cache.putInCache(request,result);
	}

	@Override
	public ClientHandler connect(Socket socket) {
		System.out.println("Connection to client " + socket.getInetAddress().toString() + " established");
		return new TCPClientHandler(socket,this);
	}
}
