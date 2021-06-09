package de.thm.mni.swtp.cs.lsp4jtest.diagnostic;

import de.thm.mni.swtp.cs.lsp4jtest.LSP4JServer;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LSP4j server that can be used for diagnostics.
 * It does not do anything meaningful on its own, but logs all requests sent by the client.
 */
public class DiagnosticServer  implements LanguageServer, LanguageClientAware {

    private static Logger logger = Logger.getLogger(DiagnosticServer.class.getName());
    private static String version = "0.0.1";
    private CompletableFuture<Object> shutdown;

    private LanguageClient client;

    public DiagnosticServer() {
        this.shutdown = new CompletableFuture<>();
    }

    public void waitForShutdown() throws ExecutionException, InterruptedException {
        this.shutdown.get();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        CompletableFuture<InitializeResult> res = new CompletableFuture<InitializeResult>();
        ServerCapabilities cap = new ServerCapabilities();
        cap.setTextDocumentSync(TextDocumentSyncKind.None);
        ServerInfo info = new ServerInfo(getClass().getSimpleName(), version);
        InitializeResult init = new InitializeResult(cap, info);
        logger.log(Level.INFO, "DiagnosticServer was initialized");
        res.complete(init);
        return res;
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        logger.log(Level.INFO, "Server was requested to shut down");
        shutdown.complete(null);
        return shutdown;
    }

    @Override
    public void exit() {
        logger.log(Level.INFO, "Server was requested to exit");
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

    public static void main(String[] args) throws Exception {
        DiagnosticServer server = new DiagnosticServer();
        // create small method that creates server socket and logs that it has done so
        Callable<ServerSocket> loggingSocketSupplier = () -> {
            ServerSocket socket = new ServerSocket(6667);
            logger.log(Level.INFO, String.format("Server socket listening on port %s", socket.getLocalPort()));
            return socket;
        };
        // use try with resources to guarantee that we close the sockets
        try (
            ServerSocket socket = loggingSocketSupplier.call();
            Socket connection = socket.accept();
        ) {
            logger.log(Level.INFO, String.format("Server connected to %s on port %d", connection.getInetAddress(), connection.getPort()));
            ExecutorService executor = Executors.newFixedThreadPool(4);
            Launcher<LanguageClient> launcher = new LSPLauncher.Builder<LanguageClient>()
                    .setLocalService(server)
                    .setRemoteInterface(LanguageClient.class)
                    .setInput(connection.getInputStream())
                    .setOutput(connection.getOutputStream())
                    .setExecutorService(executor)
                    .create();
            LanguageClient client = launcher.getRemoteProxy();
            ((LanguageClientAware) server).connect(client);
            Future<Void> launcherStopped = launcher.startListening();
            logger.log(Level.INFO, "Server launcher started listening");
            server.waitForShutdown();
            logger.log(Level.INFO, "Server shut down");
            connection.shutdownInput();
            logger.log(Level.INFO, "Socket connection shut down");
            executor.shutdown();
            logger.log(Level.INFO, "Thread pool shut down");
            launcherStopped.get();
            logger.log(Level.INFO, "Launcher shut down");
        }
        // sockets get closed due to try with resources
        logger.log(Level.INFO, String.format("Sockets closed"));
    }
}
