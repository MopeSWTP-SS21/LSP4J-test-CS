package de.thm.mni.swtp.cs.lsp4jtest;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public class LSP4jServer implements LanguageServer, LanguageClientAware {

    private LanguageClient client;

    public void doSomething() {
        System.out.println(String.format("Server thread %s sent a message", Thread.currentThread().getName()));
        System.out.flush();
        MessageParams message = new MessageParams();
        message.setMessage("Hello world!");
        message.setType(MessageType.Info);
        client.showMessage(message);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        CompletableFuture<InitializeResult> res = new CompletableFuture<InitializeResult>();
        res.complete(new InitializeResult());
        System.out.println("LSP4J server was initialized");
        return res;
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        System.out.println("LSP4J server was requested to shut down");
        return CompletableFuture.completedFuture(new Object());
    }

    @Override
    public void exit() {
        System.out.println("LSP4J server was requested to exit");
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return new TextDocumentService() {
            @Override
            public void didOpen(DidOpenTextDocumentParams params) {

            }

            @Override
            public void didChange(DidChangeTextDocumentParams params) {

            }

            @Override
            public void didClose(DidCloseTextDocumentParams params) {

            }

            @Override
            public void didSave(DidSaveTextDocumentParams params) {

            }
        };
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return new WorkspaceService() {
            @Override
            public void didChangeConfiguration(DidChangeConfigurationParams params) {

            }

            @Override
            public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {

            }
        };
    }

    public static void startClient() {
        try {
            LanguageClient client = new LSP4jClient();
            Socket socket = new Socket("127.0.0.1", 6667);
            System.out.println("Client socket connected");
            System.out.flush();
            Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(client, socket.getInputStream(), socket.getOutputStream());
            launcher.startListening();
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
            Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, connection.getInputStream(), connection.getOutputStream());
            LanguageClient client = launcher.getRemoteProxy();
            ((LanguageClientAware) server).connect(client);
            launcher.startListening();
            server.doSomething();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting LSP4J test");
        Thread startServer = new Thread(LSP4jServer::startServer);
        Thread startClient = new Thread(LSP4jServer::startClient);
        // WARNING: Using sleep() this way is stupid, please use Futures or other concurrency features in a real application!
        startServer.start();
        Thread.sleep(1000);
        startClient.start();
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;

    }
}
