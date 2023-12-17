package Client;

import cmd.BNodes;
import cmd.Chunk;
import cmd.ConnectionTCP;
import cmd.FileManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class NodeSend implements Runnable{
    private ConnectionTCP con;
    private String hostName;
    private int chunk;
    private int length;
    private ReentrantLock lock;
    private byte[] sha_1;
    private DNStable dns;
    private String path;
    private boolean received = false;

    public  NodeSend(ConnectionTCP con, String ip, int chunks, int length, String path, ReentrantLock lock, byte[] sha1, DNStable dns){
        this.hostName = ip;
        this.chunk = chunks;
        this.length = length;
        this.con = con;
        this.path = path;
        this.lock = lock;
        this.sha_1 = sha1;
        this.dns = dns;
    }

    @Override
    public void run() {
        System.out.println("Thread " + Thread.currentThread().getId() + " is executing. Host: " + this.hostName  + ", Chunk: " + chunk);

        try {
            BNodes rcvd = null;
            //boolean received = false;
            int maxRetries = 3; // Número máximo de tentativas
            int retryCount = 0;

            do {
                DatagramSocket clientSocket = new DatagramSocket();

                InetAddress serverAddress;
                if(this.dns.containsKey(this.hostName)){
                    String ip = this.dns.getIP(this.hostName);
                    serverAddress  = InetAddress.getByName(ip);
                } else {
                    serverAddress = InetAddress.getByName(this.hostName);
                    this.dns.insertIP(this.hostName, serverAddress.getHostAddress());
                }

                byte[] asd = path.getBytes(StandardCharsets.UTF_8);
                BNodes tosend = new BNodes(asd, asd.length, chunk, length, (byte) 1);
                byte[] sendData = BNodes.toByteArray(tosend);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 9090);

                lock.lock();
                try {
                    clientSocket.send(sendPacket);
                } finally {
                    lock.unlock();
                }

                // Configurar timeout
                final long timeoutMillis = 5000; // 5 segundos
                final CountDownLatch receiveSignal = new CountDownLatch(1);

                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                executor.schedule(() -> {
                    if (!received) {
                        System.out.println("Timeout: No response received on Thread" + Thread.currentThread().getId() + ". Retrying...");
                        clientSocket.close(); // Close the socket to interrupt the blocking receive
                    }
                    receiveSignal.countDown();
                }, timeoutMillis, TimeUnit.MILLISECONDS);

                try {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                    clientSocket.receive(receivePacket);
                    rcvd = BNodes.readByteArray(receivePacket.getData());
                    received = true;
                } catch (SocketTimeoutException e) {
                    // Timeout occurred, retry
                    retryCount++;
                } catch (SocketException e){}
                finally {
                    executor.shutdownNow(); // Shutdown the executor service
                }

            } while (!received && retryCount < maxRetries);

            if (!received) {
                System.out.println("Maximum retries reached. No response received.");
                // Handle the case where maximum retries are reached
            } else {
                // Process the received data
                String filename = FileManager.getFileName(path);
                FileManager.writeBytesToFile(rcvd.getData(), filename, Chunk.findOffsetStartFromIndex(rcvd.getNchunk()));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] convertToSHA1(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        return sha1Digest.digest(input);
    }
}
