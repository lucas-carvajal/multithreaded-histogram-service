package de.uniba.wiai.dsg.pks.assignment3.histogram.socket.server;

import de.uniba.wiai.dsg.pks.assignment3.histogram.socket.shared.ParseDirectory;

import java.util.concurrent.Phaser;

/**
 * Directory Runnable for registering phaser when initialized. Processes directories
 */

public class DirectoryRunnable implements Runnable {
    private final TCPClientHandler clientHandler;
    private final Phaser phaser;
    private final ParseDirectory parseDirectory;

    public DirectoryRunnable (Phaser phaser, TCPClientHandler clientHandler, ParseDirectory parseDirectory){
        phaser.register();
        this.phaser = phaser;
        this.clientHandler = clientHandler;
        this.parseDirectory = parseDirectory;
    }

    @Override
    public void run() {
        clientHandler.process(this.parseDirectory);
        phaser.arrive();
    }
}
