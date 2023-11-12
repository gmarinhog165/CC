package Server;

import cmd.Chunk;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable{
    private Socket clientSocket;
    private FS_Tracker server;

    public ClientHandler(Socket clientSocket, FS_Tracker server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();

            // forma de ler dados do cliente
            byte[] buffer = new byte[1000]; // chega um chunk de cada vez por isso podia ser mais, mas cada so tem 1000bytes
            int bytesRead;
            // List<Chunk> chunks = new ArrayList<>(); no caso de receber mais do que um chunk, mas acho q nao
            while ((bytesRead = in.read(buffer)) != -1) {
                // dar deserialize para chunk
                Chunk data = Chunk.readByteArray(Arrays.copyOf(buffer, bytesRead));
                System.out.println("Received chunk with message " + new String(data.getData()) + " from IP: " + clientSocket.getInetAddress());


                // MESSAGE MANAGER
                byte msg = data.getMsg();
                if(msg == (byte) 1){
                    this.server.writeFileOnHashMsg1(data, clientSocket.getInetAddress().getHostAddress());
                }
                else if(msg == (byte) 8){
                    Chunk tmp = new Chunk((byte) 8);
                    out.write(Chunk.toByteArray(tmp));
                    out.flush();
                    this.server.deleteNode(clientSocket.getInetAddress().getHostAddress());
                    break;
                }
                else if(msg == (byte) 3){
                    String file = new String(data.getData());
                    if(server.contains(file)){
                        List<Chunk> chunks = Chunk.fromByteArray(serializeMap(this.server.getInfoFile(file)), (byte) 5);
                        for(Chunk c : chunks){
                            out.write(Chunk.toByteArray(c));
                            out.flush();
                            int bitsread = in.read(buffer); // ACK para nao perder cenas
                        }
                    }
                    else{
                        Chunk naoExiste = new Chunk((byte) 7);
                        out.write(Chunk.toByteArray(naoExiste));
                        out.flush();
                    }

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
    private byte[] serializeMap(Map<Integer, List<String>> map) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(map);
            return byteArrayOutputStream.toByteArray();
        }
    }

}
