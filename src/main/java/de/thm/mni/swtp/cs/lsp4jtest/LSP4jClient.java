package de.thm.mni.swtp.cs.lsp4jtest;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LSP4jClient implements LanguageClient {

    private LanguageServer server;

    public void connect(LanguageServer server) {
        this.server = server;
    }

    @Override
    public void telemetryEvent(Object object) {

    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {

    }

    @Override
    public void showMessage(MessageParams messageParams) {
        System.out.println(String.format("Cliend thread %s received a message:", Thread.currentThread().getName()));
        System.out.println(messageParams.getMessage());
        System.out.flush();
        ExecuteCommandParams execute = new ExecuteCommandParams();
        execute.setCommand("hello");
        execute.setArguments(List.of("world"));
        CompletableFuture<Object> futureRes = server.getWorkspaceService().executeCommand(execute);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Object result = futureRes.get();
                    System.out.println(String.format("Client thread %s received answer to execute command:", Thread.currentThread().getName()));
                    System.out.println(result);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
        return null;
    }

    @Override
    public void logMessage(MessageParams message) {

    }
}
