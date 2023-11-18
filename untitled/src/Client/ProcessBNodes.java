package Client;

import cmd.BNodes;
import cmd.Chunk;
import cmd.FileManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class ProcessBNodes implements Runnable{
    private InetAddress ip;
    private int port;

    private byte[] bnode;

    public ProcessBNodes(InetAddress ip, int port, byte[] bnode) {
        this.ip = ip;
        this.port = port;
        this.bnode = bnode;
    }

    @Override
    public void run() {
        try {
            BNodes tmp = BNodes.readByteArray(this.bnode);
            String path = new String(tmp.getData(), StandardCharsets.UTF_8);
            int nchunk = tmp.getNchunk();
            int offset = Chunk.findOffsetStartFromIndex(nchunk);
            byte[] chunk = FileManager.readBytesFromFile(path,offset, tmp.getReadlen());
            BNodes tosend = new BNodes(chunk, chunk.length, nchunk, 0, (byte) 2);
            byte[] sendData = BNodes.toByteArray(tosend);
            DatagramSocket clientSocket = new DatagramSocket();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
            clientSocket.send(sendPacket);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
