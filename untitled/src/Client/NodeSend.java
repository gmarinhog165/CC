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
import java.util.concurrent.locks.ReentrantLock;

public class NodeSend implements Runnable{
    private ConnectionTCP con;
    private String ip;
    private int chunk;
    private int length;
    private ReentrantLock lock;
    private byte[] sha_1;

    private String path;

    public NodeSend(ConnectionTCP con, String ip, int chunks, int length, String path, ReentrantLock lock, byte[] sha1){
        this.ip = ip;
        this.chunk = chunks;
        this.length = length;
        this.con = con;
        this.path = path;
        this.lock = lock;
        this.sha_1 = sha1;
    }

    @Override
    public void run() {
        System.out.println("Thread " + Thread.currentThread().getId() + " is executing. IP: " + ip + ", Chunk: " + chunk);

        try {
            BNodes rcvd = null;
            do{
                // enviar pedido de info do chunk que quer
                DatagramSocket clientSocket = new DatagramSocket();
                InetAddress serverAddress = InetAddress.getByName(this.ip);
                byte[] asd = path.getBytes(StandardCharsets.UTF_8);
                BNodes tosend = new BNodes(asd, asd.length, chunk, length, (byte) 1);
                byte[] sendData = BNodes.toByteArray(tosend);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 9090);

                lock.lock();
                try{
                    clientSocket.send(sendPacket);
                } finally{
                    lock.unlock();
                }

                // receber a info do chunk
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);
                rcvd = BNodes.readByteArray(receivePacket.getData());
            } while(!Arrays.equals(convertToSHA1(rcvd.getData()), this.sha_1));


            // escrever no file o chunk
            String filename = FileManager.getFileName(path);
            FileManager.writeBytesToFile(rcvd.getData(), filename, Chunk.findOffsetStartFromIndex(rcvd.getNchunk()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }


    }

    private byte[] convertToSHA1(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        return sha1Digest.digest(input);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
