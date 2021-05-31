package de.thm.mni.swtp.cs.lsp4jtest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompleatableFutureExample {
    public static void main(String[] args) {
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
    }
}
