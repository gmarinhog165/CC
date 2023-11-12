package Client;

import java.util.List;

public class NodeSend implements Runnable{
    private String ip;
    private int chunk;

    public NodeSend(String ip, int chunks){
        this.ip = ip;
        this.chunk = chunks;
    }

    @Override
    public void run() {
        System.out.println("Thread " + Thread.currentThread().getId() + " is executing. IP: " + ip + ", Chunk: " + chunk);
    }
}
