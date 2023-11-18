package Client;

import Server.FS_Tracker;
import cmd.BNodes;
import cmd.Chunk;
import cmd.FileManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.List;

public class NodeSend implements Runnable{
    private Socket socket;
    private String ip;
    private int chunk;
    private int length;

    private String path;

    public NodeSend(Socket socket, String ip, int chunks, int length, String path){
        this.ip = ip;
        this.chunk = chunks;
        this.length = length;
        this.socket = socket;
        this.path = path;
    }

    @Override
    public void run() {
        System.out.println("Thread " + Thread.currentThread().getId() + " is executing. IP: " + ip + ", Chunk: " + chunk);

        try {
            // enviar pedido de info do chunk que quer
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(this.ip);
            BNodes tosend = new BNodes(path.getBytes(), path.getBytes().length, chunk, length, (byte) 1);
            byte[] sendData = BNodes.toByteArray(tosend);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 9090);
            clientSocket.send(sendPacket);
            // receber a info do chunk
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            BNodes rcvd = BNodes.readByteArray(receivePacket.getData());
            // escrever no file o chunk
            String filename = FileManager.getFileName(path);
            FileManager.writeBytesToFile(rcvd.getData(), filename, Chunk.findOffsetStartFromIndex(rcvd.getNchunk()));
            // avisar o tracker que já tem este chunk
            Chunk toTrackerMsg0 = new Chunk(path.getBytes(), 0, 0, true, (byte) 0, chunk);
            OutputStream out = socket.getOutputStream();
            out.write(Chunk.toByteArray(toTrackerMsg0));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
