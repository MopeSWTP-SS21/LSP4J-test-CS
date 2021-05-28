package de.thm.mni.swtp.cs.lsp4jtest;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

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

            public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
                System.out.println(String.format("Server thread %s was requested to execute a command:", Thread.currentThread().getName()));
                System.out.println(String.format("Command:   %s", params.getCommand()));
                System.out.println(String.format("Arguments: %s", params.getArguments()));
                System.out.flush();
                return CompletableFuture.completedFuture("Good to see you!");
            }
        };
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;

    }
}
