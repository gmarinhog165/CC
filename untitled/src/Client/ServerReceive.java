package Client;

import cmd.Chunk;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ServerReceive implements Runnable{
    private Socket socket;

    public ServerReceive(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {

        try{
            InputStream in = socket.getInputStream();
            byte[] receiveBuffer = new byte[2000];
            int bytesRead;

            List<Chunk> chunksDoMap = new ArrayList<>();
            while ((bytesRead = in.read(receiveBuffer)) != -1) {
                byte[] receivedData = Arrays.copyOf(receiveBuffer, bytesRead);
                Chunk data = Chunk.readByteArray(receivedData);
                if(data.getMsg() == (byte) 5)
                    continue;

                chunksDoMap.add(data);
                // quando chegar o último chunk começar o processo
                if(data.isLast()){
                    messageManager(chunksDoMap);
                    chunksDoMap.clear();
                }
            }

        } catch(IOException e){
            e.printStackTrace();
        } catch (ClassNotFoundException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Método responsável por criar threads para buscarem chunks a outros nodes.
     * estou a assumir que só recebe do tracker a mensagem do algoritmo
     * @param data
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    public void messageManager(List<Chunk> data) throws IOException, ClassNotFoundException, InterruptedException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // juntar todos os chunks de data correspondentes
        for(Chunk c : data){
            outputStream.write(c.getData());
        }
        // dar deserialize do byte[]
        Map<String, List<Integer>> locs = deserializeMap(outputStream.toByteArray());

        // verificar len para criar um array com todas as threads necessárias para as
        // poder inicializar simultaneamente
        int len = locs.values()
                .stream()
                .mapToInt(List::size)
                .sum();

        Thread[] threads = new Thread[len];
        int i = 0;
        for(Map.Entry<String, List<Integer>> d : locs.entrySet()){
            String ip = d.getKey();
            List<Integer> chunks = d.getValue();
            for(int b : chunks){
                threads[i] = new Thread(new NodeSend(ip, b));
                threads[i++].start();
            }

        }

        for(int j = 0; j < len; j++){
            threads[j].join();
        }
    }

    /**
     * Método para converter byte[] em Map<String, List<Integer>> que é o tipo de dados que o algoritmo gera
     * @param serializedData
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Map<String, List<Integer>> deserializeMap(byte[] serializedData) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedData);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (Map<String, List<Integer>>) objectInputStream.readObject();
        }
    }
}
