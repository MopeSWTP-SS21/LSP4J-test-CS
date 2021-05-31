package de.thm.mni.swtp.cs.lsp4jtest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketCloseTest {
    public static void main(String[] args) throws InterruptedException {
        Thread server = new Thread(() -> {
            try {
                ServerSocket serverSock = new ServerSocket(6667);
                Socket socket = serverSock.accept();
                socket.getOutputStream().write(0);
                socket.getOutputStream().write(1);
                socket.getOutputStream().write(2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Thread client = new Thread(() -> {
            try {
                Socket socket = new Socket("127.0.0.1", 6667);
                int b = socket.getInputStream().read();
                b = socket.getInputStream().read();
                b = socket.getInputStream().read();
                socket.getInputStream().close();
                b = socket.getInputStream().read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server.start();
        Thread.sleep(1000);
        client.start();
    }
}
