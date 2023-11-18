package Client;

import cmd.BNodes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class NodeHandler implements Runnable{
    public NodeHandler() {
    }

    @Override
    public void run() {
        try{
            DatagramSocket serverSocket = new DatagramSocket(9090);
            byte[] receiveData = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                Thread go = new Thread(new ProcessBNodes(receivePacket.getAddress(), receivePacket.getPort(), receivePacket.getData()));
                go.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
