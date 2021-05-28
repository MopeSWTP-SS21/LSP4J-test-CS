package de.thm.mni.swtp.cs.lsp4jtest;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;

import java.util.concurrent.CompletableFuture;

public class LSP4jServer implements LanguageServer {
    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        return null;
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return null;
    }

    @Override
    public void exit() {

    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return null;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return null;
    }

    public static void main(String[] args) {
        LSP4jServer server = new LSP4jServer();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, inputStream, outputStream);
        LanguageClient client = launcher.getRemoteProxy();
        ((LanguageClientAware) server).connect(client);
        launcher.startListening();
    }
}