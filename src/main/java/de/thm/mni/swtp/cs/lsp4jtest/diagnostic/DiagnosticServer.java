package de.thm.mni.swtp.cs.lsp4jtest.diagnostic;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.generator.JsonRpcData;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonDelegate;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * LSP4j server that can be used for diagnostics.
 * It does not do anything meaningful on its own, but logs all requests sent by the client.
 */
public class DiagnosticServer  implements LanguageServer, LanguageClientAware {

    private static Logger logger = Logger.getLogger(DiagnosticServer.class.getName());
    private static Map<Level, MessageType> levelMap = Map.of(
            Level.INFO, MessageType.Info,
            Level.WARNING, MessageType.Warning,
            Level.SEVERE, MessageType.Error
    );
    private static String version = "0.0.1";
    private CompletableFuture<Object> shutdown;
    private Map<String, List<String>> documents = new HashMap<>();

    private LanguageClient client;

    public DiagnosticServer() {
        this.shutdown = new CompletableFuture<>();
        // ensures that all logged messages are forwarded to the client if possible
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (client == null || !isRunning()) { return; }
                client.showMessage(new MessageParams(levelMap.getOrDefault(record.getLevel(), MessageType.Log), record.getMessage()));
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        });
    }

    public void waitForShutdown() throws ExecutionException, InterruptedException {
        this.shutdown.get();
    }

    @JsonRpcData
    public class LoadModelParams {
        @NonNull
        public String modelName;
        public LoadModelParams(String modelName) {
            this.modelName = modelName;
        }
    }

    @JsonSegment("modelica")
    public interface ModelicaService {
        @JsonRequest
        CompletableFuture<String> loadModel(String modelName);

        @JsonRequest
        CompletableFuture<String> loadModelFixed(LoadModelParams params);
    }

    @JsonDelegate
    public ModelicaService getModelicaService() {
        return new ModelicaService() {
            public CompletableFuture<String> loadModel(String modelName){
                logger.log(Level.INFO, String.format("loadModel(%s)", modelName));
                return CompletableFuture.completedFuture(String.format("loaded %s", modelName));
            }
            public CompletableFuture<String> loadModelFixed(LoadModelParams params){
                logger.log(Level.INFO, String.format("loadModel(%s)", params.modelName));
                return CompletableFuture.completedFuture(String.format("loaded %s", params.modelName));
            }
        };
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        logger.log(Level.INFO, String.format(
                "Client %s@%s requested initialization with the following capabilities:\n%s",
                params.getClientInfo().getName(),
                params.getClientInfo().getVersion(),
                params.getCapabilities()
        ));
        CompletableFuture<InitializeResult> res = new CompletableFuture<InitializeResult>();
        ServerCapabilities cap = new ServerCapabilities();
        TextDocumentSyncOptions sync = new TextDocumentSyncOptions();
        sync.setOpenClose(true);
        sync.setChange(TextDocumentSyncKind.Incremental);
        sync.setSave(new SaveOptions(true));
        cap.setTextDocumentSync(sync);
        // Completion capabilities
        CompletionOptions comp = new CompletionOptions();
        comp.setTriggerCharacters(List.of("."));
        comp.setResolveProvider(false);
        cap.setCompletionProvider(comp);
        // Workspace capabilities
        WorkspaceServerCapabilities workspace = new WorkspaceServerCapabilities();
        WorkspaceFoldersOptions workspaceFolders = new WorkspaceFoldersOptions();
        workspaceFolders.setSupported(true);
        workspaceFolders.setChangeNotifications(true);
        workspace.setWorkspaceFolders(workspaceFolders);
        // register commands that can be executed
        cap.setExecuteCommandProvider(new ExecuteCommandOptions(List.of(
                "loadModel"
        )));
        cap.setWorkspace(workspace);
        logger.log(Level.INFO, cap.toString());
        ServerInfo info = new ServerInfo(getClass().getSimpleName(), version);
        InitializeResult init = new InitializeResult(cap, info);
        logger.log(Level.INFO, "DiagnosticServer was initialized");
        res.complete(init);
        return res;
    }

    @Override
    public void initialized(InitializedParams params) {
        logger.log(Level.INFO, "Client has received initialize result");
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        logger.log(Level.INFO, "Server was requested to shut down");
        // FIXME this should stop the server from sending commands, but not from *receiving* commands
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
            public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
                logger.log(Level.INFO, String.format(
                        "completion: %s:%d:%d %s(%s)",
                        position.getTextDocument().getUri(),
                        position.getPosition().getLine(),
                        position.getPosition().getCharacter(),
                        position.getContext().getTriggerKind(),
                        position.getContext().getTriggerKind()
                ));
                CompletionList lst = new CompletionList();
                lst.setIsIncomplete(false);
                CompletionItem item1 = new CompletionItem("dummy1");
                item1.setDocumentation("This is just a dummy entry");
                CompletionItem item2 = new CompletionItem("dummy2");
                item2.setDocumentation("This is just a dummy entry");
                lst.setItems(List.of(item1, item2));
                return CompletableFuture.completedFuture(Either.forRight(lst));
            }

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

            @Override
            public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
                String args = params.getArguments().stream().map(Object::toString).collect(Collectors.joining(", "));
                logger.log(Level.INFO, String.format("executeCommand: %s(%s)", params.getCommand(), args));
                return CompletableFuture.completedFuture("Sorry, I cannot do that. I am just a dummy. ^^\"");
            }

            @Override
            public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
                logger.log(Level.INFO, String.format(
                        "didChangeWorkspaceFolders:\nAdded:\n%s\n\nRemoved:\n%s",
                        params.getEvent().getAdded(),
                        params.getEvent().getRemoved()
                ));
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
        logger.log(Level.INFO, "Sockets closed");
    }
}
