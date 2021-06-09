package de.thm.mni.swtp.cs.lsp4jtest.diagnostic;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
                logger.log(Level.INFO, String.format("didOpen: %s", params.getTextDocument().getUri()));
            }

            @Override
            public void didChange(DidChangeTextDocumentParams params) {
                logger.log(Level.INFO, String.format("didChange: %s", params.getTextDocument().getUri()));
            }

            @Override
            public void didClose(DidCloseTextDocumentParams params) {
                logger.log(Level.INFO, String.format("didClose: %s", params.getTextDocument().getUri()));
            }

            @Override
            public void didSave(DidSaveTextDocumentParams params) {
                logger.log(Level.INFO, String.format("didSave: %s", params.getTextDocument().getUri()));
            }
        };
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return new WorkspaceService() {
            @Override
            public void didChangeConfiguration(DidChangeConfigurationParams params) {
                logger.log(Level.INFO, String.format("didChangeConfiguration: %s", params.getSettings()));
            }

            @Override
            public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
                List<String> lst = params.getChanges().stream().map(
                        x -> String.format("%s: %s", x.getUri(), x.getType())
                ).collect(Collectors.toList());
                logger.log(Level.INFO, String.format("didChangeWatchedFiles: %s", lst));
            }

            public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
                String args = params.getArguments().stream().map(Object::toString).collect(Collectors.joining(", "));
                logger.log(Level.INFO, String.format("executeCommand: %s(%s)", params.getCommand(), args));
                return CompletableFuture.completedFuture("Sorry, I cannot do that. I am just a dummy. ^^\"");
            }
        };
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;

    }

    public static void stopFromConsole(DiagnosticServer server) {
        System.out.println("Press enter to stop the server prematurely");
        try {
            // wait in a polling loop until either server is shutdown or System.in has input
            while (server.isRunning() && System.in.available() == 0) {
                Thread.sleep(100);
            }
        } catch (IOException | InterruptedException e) {
            /* ignore because we just need any signal to shut down */
        }
        server.shutdown();
    }

    private boolean isRunning() {
        return !shutdown.isDone();
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
            new Thread(() -> stopFromConsole(server)).start();
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
