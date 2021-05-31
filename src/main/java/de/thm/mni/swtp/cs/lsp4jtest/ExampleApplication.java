package de.thm.mni.swtp.cs.lsp4jtest;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class ExampleApplication {
    public static void startClient(CompletableFuture<Void> shutdown, CompletableFuture<Void> serverReady) {
        try {
            LSP4JClient client = new LSP4JClient();
            // NOTE: We need to wait for the server socket to enter listening mode
            serverReady.get();
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
            shutdown.get(); // wait for shutdown
            socket.shutdownInput();
            System.out.println("Client shutting down");
            System.out.flush();
            executor.shutdown();
            future.get();
            socket.close();
        } catch (IOException | InterruptedException | ExecutionException e) {
            // Ignore, because we want to shutdown anyway
            // IOException means that connection was not established => we need to shut down
            // ExecutionException cannot occur, because we never call completeExceptionally in any thread
            // InterruptedException cannot not occur, because we do not call Thread.interrupt anywhere
            e.printStackTrace();
        }
    }

    public static void startServer(CompletableFuture<Void> shutdown, CompletableFuture<Void> serverReady) {
        try {
            LSP4JServer server = new LSP4JServer();
            ServerSocket socket = new ServerSocket(6667);
            System.out.println("Server socket listening");
            System.out.flush();
            // NOTE: server socket is already listening after it's constructor returns
            //       => it is safe to signal server readiness here
            serverReady.complete(null);
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
            shutdown.get(); // wait for shutdown
            connection.shutdownInput();
            System.out.println("Server shutting down");
            System.out.flush();
            executor.shutdown();
            future.get();
            connection.close();
            socket.close();
        } catch (IOException | InterruptedException | ExecutionException e) {
            // Ignore, because we want to shutdown anyway
            // IOException means that connection was not established => we need to shut down
            // ExecutionException cannot occur, because we never call completeExceptionally in any thread
            // InterruptedException cannot not occur, because we do not call Thread.interrupt anywhere
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting LSP4J test");
        CompletableFuture<Void> shutdown = new CompletableFuture<>();
        CompletableFuture<Void> serverReady = new CompletableFuture<>();
        Thread startServer = new Thread(() -> startServer(shutdown, serverReady));
        Thread startClient = new Thread(() -> startClient(shutdown, serverReady));
        startServer.start();
        startClient.start();
        System.out.println("Press enter to stop server execution");
        System.in.read();
        shutdown.complete(null);
    }
}
