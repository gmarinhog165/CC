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
                DatagramPacket receivePacket = new DatagramPacket(receiveData, 1024);
                serverSocket.receive(receivePacket);
                byte[] asd = receivePacket.getData();
                BNodes c = BNodes.readByteArray(asd);
                Thread go = new Thread(new ProcessBNodes(receivePacket.getAddress(), receivePacket.getPort(), c));
                go.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
