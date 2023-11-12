package Client;

import Server.FS_Tracker;

import java.net.*;
import java.util.List;

public class NodeSend implements Runnable{
    private Socket socket;
    private String ip;
    private int chunk;
    private int length;

    public NodeSend(Socket socket, String ip, int chunks, int length){
        this.ip = ip;
        this.chunk = chunks;
        this.length = length;
        this.socket = socket;
    }

    @Override
    public void run() {
        System.out.println("Thread " + Thread.currentThread().getId() + " is executing. IP: " + ip + ", Chunk: " + chunk);

        try {
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 9090;


        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }


    }
}
