package de.thm.mni.swtp.cs.lsp4jtest;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;

public class NoExceptionSocketInputStream extends InputStream {

    private Socket socket;

    public NoExceptionSocketInputStream(Socket socket) {
        this.socket = socket;
    }

    @Override
    public int read() throws IOException {
        if (socket.isClosed()) { return -1; }
        try {
            return socket.getInputStream().read();
        } catch (SocketException e) {
            if (e.getMessage().contains("closed")) {
                // NOTE: the exact message is implementation dependent, but
                // it seems safe to assume that an error due to a closed socket
                // will always contain the word closed and other erros are
                // unlikely to contain this word.
                return -1;
            }
            throw e;
        }
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
