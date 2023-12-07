package Server;

import cmd.Chunk;
import cmd.ConnectionTCP;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable{
    private Socket clientSocket;
    private FS_Tracker server;
    private ConnectionTCP con;

    public ClientHandler(Socket clientSocket, FS_Tracker server) throws IOException {
        this.clientSocket = clientSocket;
        this.server = server;
        this.con = new ConnectionTCP(clientSocket);
    }

    @Override
    public void run() {
        try {
            while (true) {
                // dar deserialize para chunk
                Chunk data = con.receive();
                System.out.println("Received chunk with message " + new String(data.getData()) + " from: " + clientSocket.getInetAddress().getHostName());


                // MESSAGE MANAGER

                // file
                byte msg = data.getMsg();
                if(msg == (byte) 1){
                    this.server.writeFileOnHashMsg1(data, clientSocket.getInetAddress().getHostName());
                }

                else if(msg == (byte) 11){
                    List<String> tmp = this.server.getAllHosts();
                    con.send(new Chunk(new byte[0], 0, tmp.size(), true, (byte) 11));
                    for(String c : tmp){
                        byte[] dat = c.getBytes();
                        con.send(new Chunk(dat,dat.length,0,false,(byte) 11));
                        con.receive();
                    }
                }

                //lista sha-1
                else if (msg == (byte) 2){
                    List<Chunk> tmp = new ArrayList<>();
                    int limit = data.getOffset();
                    String path = new String(data.getData());

                    for(int k = 0; k < limit; k++){
                        Chunk c = con.receive();
                        tmp.add(c);
                        con.send(new Chunk((byte) 9));
                    }
                    this.server.insertSH1(tmp, path);
                }

                else if(msg == (byte) 8){
                    con.send(new Chunk((byte) 8));
                    this.server.deleteNode(clientSocket.getInetAddress().getHostName());
                    break;
                }

                else if(msg == (byte) 3){
                    String file = new String(data.getData());
                    if(server.contains(file)){
                        List<Chunk> chunks = Chunk.fromByteArray(serializeMap(this.server.getInfoFile(file)), (byte) 5);
                        con.send(new Chunk(new byte[0], chunks.size(), 0, false, (byte) 3));
                        for(Chunk c : chunks){
                            con.send(c); // ACK para nao perder cenas
                            con.receive();
                        }

                        // enviar SHA-1s
                        Map<Integer, byte[]> map = this.server.getSHA1(file);
                        byte[] smap = serializeSHA1(map);
                        List<Chunk> tmp2 = Chunk.fromByteArray(smap, (byte) 6);
                        int len = tmp2.size();
                        con.send(new Chunk(new byte[0], tmp2.size(), 0, false, (byte) 6));
                        for(Chunk c : tmp2){
                            con.send(c); // ACK para nao perder cenas
                            con.receive();
                        }
                    }
                    else{
                        con.send(new Chunk((byte) 7));
                    }
                }

                else if (msg == (byte) 0){
                    this.server.writeChunkOnHashMsg0(data, clientSocket.getInetAddress().getHostName());
                    con.send(new Chunk((byte) 9));
                }
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Método para converter Map<String, List<Integer>> do algoritmo em byte[]
     * @param map
     * @return
     * @throws IOException
     */
    private byte[] serializeMap(Map<Integer, Set<String>> map) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(map);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private byte[] serializeSHA1(Map<Integer, byte[]> map) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(map);
            return byteArrayOutputStream.toByteArray();
        }
    }

}
