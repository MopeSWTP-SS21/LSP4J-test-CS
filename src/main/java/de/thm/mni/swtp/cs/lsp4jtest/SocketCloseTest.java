package de.thm.mni.swtp.cs.lsp4jtest;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketCloseTest {
    private static class NonExplodingSocketInputStream extends InputStream {

        private Socket socket;

        public NonExplodingSocketInputStream(Socket socket) {
            this.socket = socket;
        }

        @Override
        public int read() throws IOException {
            if (socket.isClosed()) { return -1; }
            return socket.getInputStream().read();
        }

        @Override
        public void close() throws IOException {
            socket.getInputStream().close();
        }
    }
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
                InputStream input = new NonExplodingSocketInputStream(socket);
                int b = input.read();
                b = input.read();
                b = input.read();
                input.close();
                b = input.read();
                System.out.println(b);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server.start();
        Thread.sleep(1000);
        client.start();
    }
}
