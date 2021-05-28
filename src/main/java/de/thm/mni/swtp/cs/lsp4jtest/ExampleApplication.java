package de.thm.mni.swtp.cs.lsp4jtest;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ExampleApplication {
    public static void startClient() {
        try {
            LSP4jClient client = new LSP4jClient();
            Socket socket = new Socket("127.0.0.1", 6667);
            System.out.println("Client socket connected");
            System.out.flush();
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Launcher<LanguageServer> launcher = new LSPLauncher.Builder<LanguageServer>()
                    .setLocalService(client)
                    .setRemoteInterface(LanguageServer.class)
                    .setInput(socket.getInputStream())
                    .setOutput(socket.getOutputStream())
                    .setExecutorService(executor)
                    .create();
            client.connect(launcher.getRemoteProxy());
            Future<Void> future = launcher.startListening();
            synchronized(clientLock) {
                try { clientLock.wait(); } catch (InterruptedException e) { /* if interrupted, we exit anyway */ }
            }
            socket.close();
            executor.shutdown();
            try { future.get(); } catch (Exception e) { /* we don't care, just exit somehow */ }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startServer() {
        try {
            LSP4jServer server = new LSP4jServer();
            ServerSocket socket = new ServerSocket(6667);
            System.out.println("Server socket listening");
            System.out.flush();
            Socket connection = socket.accept();
            System.out.println("Server connected to client socket");
            System.out.flush();
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Launcher<LanguageClient> launcher = new LSPLauncher.Builder<LanguageClient>()
                    .setLocalService(server)
                    .setRemoteInterface(LanguageClient.class)
                    .setInput(connection.getInputStream())
                    .setOutput(connection.getOutputStream())
                    .setExecutorService(executor)
                    .create();
            LanguageClient client = launcher.getRemoteProxy();
            ((LanguageClientAware) server).connect(client);
            Future<Void> future = launcher.startListening();
            server.doSomething();
            synchronized(serverLock) {
                try {
                    serverLock.wait();
                } catch (InterruptedException e) { /* if interrupted, we exit anyway */ }
            }
            socket.close();
            connection.close();
            executor.shutdown();
            try { future.get(); } catch (Exception e) { /* we don't care, just exit somehow */ }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // WARNING: There are way better/convenient methods for communication between threads in java now
    // You should not use plain locks in a real application unless you REALLY know what your're doing
    // I chose to do this, because I am familiar with the concept and it was the quickest solution for me
    private static final Object serverLock = new Object();
    private static final Object clientLock = new Object();

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting LSP4J test");
        Thread startServer = new Thread(ExampleApplication::startServer);
        Thread startClient = new Thread(ExampleApplication::startClient);
        // WARNING: Using sleep() this way is stupid, please use Futures or other concurrency features in a real application!
        startServer.start();
        Thread.sleep(1000);
        startClient.start();
        System.out.println("Press enter to stop server execution");
        System.in.read();
        synchronized (serverLock) { serverLock.notify(); }
        synchronized (clientLock) { clientLock.notify(); }
    }
}
