package de.thm.mni.swtp.cs.lsp4jtest;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class NoExceptionSocketInputStream extends InputStream {

    private Socket socket;

    public NoExceptionSocketInputStream(Socket socket) {
        this.socket = socket;
    }

    @Override
    public int read() throws IOException {
        if (socket.isClosed()) { return -1; }
        return socket.getInputStream().read();
    }

    @Override
    public int available() throws IOException {
        return socket.getInputStream().available();
    }

    @Override
    public void close() throws IOException {
        socket.getInputStream().close();
    }
}
