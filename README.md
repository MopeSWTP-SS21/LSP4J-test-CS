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

## General tips regarding concurrency in Java

### CompletableFuture

The first version of this code used low-level Java concurrency features such as `synchronized`, `Object.wait()` and `Object.notify()`.
This was sufficient to get things running, but there are high-level interfaces that make things much more convenient.
The main class that was helpful in this case was `CompletableFuture<T>`.
It provides an easy way to make one thread wait for the result of another thread.
Consider this minimal example:

```java
final CompletableFuture<Integer> future = new CompletableFuture<>();
Thread supplier = new Thread(() -> {
    try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
    try { future.complete(333); } catch (Exception e) { /*ignore*/ }
});
Thread consumer = new Thread(() -> {
    try {
        Integer x = future.get();
        System.out.println(x + 10);
    } catch (Exception e) {
        /* ignore */
    }
});
consumer.start();
supplier.start();
```

Regardless of how long it takes the `supplier` thread to calculate the value we want,  the`consumer` thread will always wait until that value is available and can then use it for further processing.

In our case, however, we do not need a value, but just need to let two threads wait for one signal.
If the purpose of a `CompletableFuture` is not to transfer a value, but just to serve as a waiting mechanism, we can use a `CompletableFuture<Void>`, which does not have any value.
The only caveat is that, because of typical Java generics shenanigans, we still need to *supply* a value if we call `complete()`.
We cannot generate any object of the type `Void` since the purpose of that type is precisely to represent "nothingness", but we can use `null`.
This seems a little awkward at first, but works perfectly fine.

### ExcecutorService

`ExecutorService`s are another great tool for concurrency in Java.
Pretty much the only situation in which you want to start a thread *without* an `ExecutorService` are the "main" application threads, which are supposed to run in near-infinite loops until the application is shut down.
Other concurrent tasks that simply calculate a result or modify the state of some objects and then terminate should use some kind of thread pool, which manages the distribution of processor time between threads and can be configured for speed and/or fairness.
`ExecutorService`s are created through the static helper class `java.util.concurrent.Executors` and can be used like this:

```java
ExecutorService pool = Executors.newFixedThreadPool(8);
// Submit Callable<Integer> via lambda syntax
Future<Integer> result = pool.submit(() -> 1+1 );
Integer x = result.get();
```

### java.util.concurrent

In general, if you have any concurrency problem, the [documentation of the package `java.util.concurrent`](https://docs.oracle.com/en/java/javase/15/docs/api/java.base/java/util/concurrent/package-summary.html) is a great start.
It offers high-level interfaces and implementations for most if not any situation.
This includes, for example:

* `ConcurrentLinkedQueue`, `LinkedBlockingQueue`, and other concurrent `Collections` that can help to pass data around safely between threads.
* `FutureTask`, which can serve as a base class for concurrent tasks that can be cancelled.
* `ReentrantLock` and other lock implementations in the subpackage `java.util.concurrent.locks`, which are somewhat low-level, but can be used to overcome a lot of concurrency issues regarding restricted access to resources.
* `CountDownLatch`, `CyclicBarrier`, and other mechanisms for more complicated thread synchronization needs.


### Properly shutting down a Socket

The `Socket closed` errors encountered during the shutdown of the LSP4J `Launcher`s can be avoided by gracefully shutting down the sockets.
The `close()` method is a rather brutal way to cut communication, since all subsequent use of the socket instance will result in `IOException`s.
If you want to signal the threads that are reading from the socket that there is nothing more to read, you can use `shutdownInput()` beforehand.
This will not close the connection, but will result in further input being ignored and the input stream reporting that it has reached end of file.
You *still* should properly close the socket after the `Launcher`s have exited, but this way you can avoid `SocketException`s in your logging output.

## References

I compiled the information required to build this project from the following sources:

* [LSP4J documentation](https://github.com/eclipse/lsp4j/blob/master/documentation/README.md)
* [This test case within the LSP4J project](https://github.com/eclipse/lsp4j/blob/master/org.eclipse.lsp4j/src/test/java/org/eclipse/lsp4j/test/LSPEndpointTest.xtend)
* By browsing through the LSP4J codebase
* [TypeFox/lsp4j-chat-app](https://github.com/TypeFox/lsp4j-chat-app) (which uses surprisingly few of the actual LSP4J interfaces and is more a JSON RPC app than an LSP4J app)
* [This issue regarding LSP4J shutdown](https://github.com/eclipse/lsp4j/issues/358)
