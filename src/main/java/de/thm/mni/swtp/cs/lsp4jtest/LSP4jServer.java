package de.thm.mni.swtp.cs.lsp4jtest;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

public class LSP4jServer implements LanguageServer {
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
        LanguageClient client = new LSP4jClient();
        LSPLauncher.createClientLauncher(client, inputStream, outputStream);
    }

    public static void startServer() throws IOException {
        LSP4jServer server = new LSP4jServer();
        ServerSocket socket = new ServerSocket(667);
        Socket connection = socket.accept();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, connection.getInputStream(), connection.getOutputStream());
        LanguageClient client = launcher.getRemoteProxy();
        ((LanguageClientAware) server).connect(client);
        launcher.startListening();
    }

    public static void main(String[] args) throws IOException {
        startServer();
        startClient();
    }
}
