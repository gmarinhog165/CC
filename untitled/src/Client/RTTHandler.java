package Client;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class RTTHandler implements Runnable {
    private DNStable dns;
    private RTTMap rtts;
    private List<String> hosts;

    public RTTHandler(DNStable dns, RTTMap rtts, List<String> hosts) {
        this.dns = dns;
        this.rtts = rtts;
        this.hosts = hosts;
    }

    @Override
    public void run() {
        try {
            DatagramSocket clientSocket = new DatagramSocket();

            for(String c : hosts){
                InetAddress serverAddress = InetAddress.getByName(c);
                this.dns.insertIP(c, serverAddress.getHostAddress());
                //System.out.println("RTTHandler:: " + c+ "   " + serverAddress.getHostAddress());
            }


            while (true) {
                List<String> hosts = this.dns.getHosts();
                int num = hosts.size();
                Thread[] n = new Thread[num];
                for (int i = 0; i < num; i++) {
                    String m = hosts.get(i);
                    n[i] = new Thread(new RTTCalculator(m, this.dns, rtts));
                    n[i].start();
                }
                Thread.sleep(5000);
            }
        } catch (InterruptedException | SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
