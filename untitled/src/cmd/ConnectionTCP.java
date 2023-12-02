package cmd;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionTCP implements AutoCloseable {
    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private ReentrantLock readl = new ReentrantLock();
    private ReentrantLock writel = new ReentrantLock();

    @Override
    public void close() throws Exception {
        System.out.println("dei exit");
        this.socket.close();
    }

    public ConnectionTCP(Socket socket) throws IOException {
        this.socket = socket;
        this.out = socket.getOutputStream();
        this.in = socket.getInputStream();
    }

    public void send(Chunk chunk) throws IOException{
        this.writel.lock();
        try{
            out.write(Chunk.toByteArray(chunk));
            out.flush();
        } finally {
            this.writel.unlock();
        }
    }

    public Chunk receive() throws IOException{
        this.readl.lock();
        try{
            byte[] buffer = new byte[1000];
            in.read(buffer);
            return Chunk.readByteArray(buffer);
        } finally{
            this.readl.unlock();
        }
    }
}
