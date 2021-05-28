# LSP4J Test project

This is a minimal test project to get familiar with LSP4J.
It is not supposed to do anything useful beyond offering a "hello world" example.

## Quick start

The project consist of three classes: `LSP4JClient` and `LSP4JServer` are minimal implementations of the main LSP4J interfaces and `ExampleApplication` contains the main method that starts the server and client threads.
To test the project, run `ExampleApplication` and observe the messages sent by the different threads in the console.
You can stop the server and client by pressing enter.

## General insights

### Remote procedure calls

LSP4J sets up remote procedure calls (RPCs) between a `LanguageServer` and a `LanguageClient` instance.
Consider this example of server side code:

```java
LSP4JServer server = new LSP4JServer();
Launcher<LanguageClient> launcher = new LSPLauncher.Builder<LanguageClient>()
        .setLocalService(server)
        .setRemoteInterface(LanguageClient.class)
        .setInput(connection.getInputStream())
        .setOutput(connection.getOutputStream())
        .setExecutorService(executor)
        .create();
LanguageClient client = launcher.getRemoteProxy();
```

The `server` object is a normal java object that receives calls from the remote client and executes Java code in the server process to handle these calls.
The `client` object is only a proxy that *looks* like and behaves like a local implementation of its interfaces, but actually forwards all calls to the corresponding object in the client process via JSON RPC.

On the client side, everything is flipped:

```java
LSP4JClient client = new LSP4JClient();
Launcher<LanguageServer> launcher = new LSPLauncher.Builder<LanguageServer>()
        .setLocalService(client)
        .setRemoteInterface(LanguageServer.class)
        .setInput(socket.getInputStream())
        .setOutput(socket.getOutputStream())
        .setExecutorService(executor)
        .create();
LanguageServer server = launcher.getRemoteProxy();
```

Here, `client` is the actual local java object, and `server` is the proxy that forwards all calls to the remote object in the server process.

### Transport layer

As you can see, the `Launcher` class involved in setting up the RPC connection accepts any old `InputStream` and `OutputStream`.
In true java fashion, this allows for a lot of flexibility in choosing how the client and server are actually connected.
The straightforward way is to use Java's `Socket` and `ServerSocket` for a TCP connection, but it is also possible to use, for example, `PipedInputStream` and `PipedOutputStream` for local testing within the same java process.

### Listening

To ensure that local setup is complete before any messages are received, the Launcher has a `.startListening()` function that can be called after the proxy objects have been passed around to where they need to be registered.

### Shutdown

Shutting down a server/client started by a `Launcher` is not straightforward.
First, you need to close its input stream so that it recognizes that there are no more messages to be received from the remote process.
This is not enough, however, as the launcher uses an internal thread pool which is not closed by default.
To be able to shutdown this thread pool it is best to actually create an own thread pool, pass it to the launcher builder via `setExecutorService()`, and then shut it down manually through the threadpool's `shutdown()` method.
