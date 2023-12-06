package Client;

import cmd.BNodes;

import java.io.IOException;
import java.net.*;
import java.util.Map;

public class RTTCalculator implements Runnable {
    private String hostName;
    private DNStable dns;
    private Map<String, Long> rtts;


    public RTTCalculator(String hostname, DNStable dns, Map<String,Long> rtts) {
        this.hostName = hostname;
        this.dns = dns;
        this.rtts=rtts;
    }

    public void run()  {

        try {
            BNodes rcvd = null;

            DatagramSocket clientSocket = new DatagramSocket();


            String ip = this.dns.getIP(this.hostName);
            InetAddress serverAddress = InetAddress.getByName(ip);

            BNodes tosend = new BNodes(new byte[0], 0, 0, 0, (byte) 10);
            byte[] sendData = BNodes.toByteArray(tosend);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 9090);
            long startTime = System.nanoTime();
            clientSocket.send(sendPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);


            long endTime = System.nanoTime();; // Mede o tempo de término
            long rtt = endTime - startTime; // Calcula o RTT
            this.rtts.put(hostName, rtt);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }



}
