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
    public static void startClient(CompletableFuture<Void> shutdown) {
        try {
            LSP4JClient client = new LSP4JClient();
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
            socket.close();
            executor.shutdown();
            try { future.get(); } catch (Exception e) { /* we don't care, just exit somehow */ }
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void startServer(CompletableFuture<Void> shutdown) {
        try {
            LSP4JServer server = new LSP4JServer();
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
            shutdown.get(); // wait for shutdown
            socket.close();
            connection.close();
            executor.shutdown();
            try { future.get(); } catch (Exception e) { /* we don't care, just exit somehow */ }
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting LSP4J test");
        CompletableFuture<Void> shutdown = new CompletableFuture<>();
        Thread startServer = new Thread(() -> startServer(shutdown));
        Thread startClient = new Thread(() -> startClient(shutdown));
        // WARNING: Using sleep() this way is stupid, please use Futures or other concurrency features in a real application!
        startServer.start();
        Thread.sleep(1000);
        startClient.start();
        System.out.println("Press enter to stop server execution");
        System.in.read();
        shutdown.complete(null);
    }
}
