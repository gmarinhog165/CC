package Client;

import cmd.Chunk;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class ServerReceive implements Runnable{
    private Socket socket;

    public ServerReceive(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {

        try{
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            byte[] receiveBuffer = new byte[1000];
            int bytesRead;

            List<Chunk> chunksDoMap = new ArrayList<>();
            while ((bytesRead = in.read(receiveBuffer)) != -1) {
                byte[] receivedData = Arrays.copyOf(receiveBuffer, bytesRead);
                Chunk data = Chunk.readByteArray(receivedData);
                if(data.getMsg() == (byte) 7){
                    System.out.println("O ficheiro que inseriu não está disponível!");
                    continue;
                }
                chunksDoMap.add(data);
                // quando chegar o último chunk começar o processo
                if(data.isLast()){
                    messageManager(chunksDoMap);
                    chunksDoMap.clear();
                }
                out.write(Chunk.toByteArray(new Chunk((byte) 9)));
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
        // TO DO: falta o caso da mensagem ser do tipo 6 -> lista de SHA-1

        if(data.get(0).getMsg() == (byte) 5){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // juntar todos os chunks de data correspondentes
            for(Chunk c : data){
                outputStream.write(c.getData());
            }
            // dar deserialize do byte[]
            Map<String, List<Integer>> locs = algoritmo(deserializeMap(outputStream.toByteArray()));

            // verificar len para criar um array com todas as threads necessárias para as
            // poder inicializar simultaneamente
            int len = locs.values()
                    .stream()
                    .mapToInt(List::size)
                    .sum();

            System.out.println(len);
            Thread[] threads = new Thread[len];
            int i = 0;
            for(Map.Entry<String, List<Integer>> d : locs.entrySet()){
                String ip = d.getKey();
                List<Integer> chunks = d.getValue();
                for(int b : chunks){
                    threads[i] = new Thread(new NodeSend(ip, b));
                    threads[i++].start();// pesquisar ticket
                }

            }

            for(int j = 0; j < len; j++){
                threads[j].join();
            }
        }
    }

    /**
     * Método para converter byte[] em Map<String, List<Integer>> que é o tipo de dados que o algoritmo gera
     * @param serializedData
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Map<Integer, List<String>> deserializeMap(byte[] serializedData) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedData);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (Map<Integer, List<String>>) objectInputStream.readObject();
        }
    }


    /**
     * Protótipo de algoritmo
     * @param chunkMap
     * @return
     */
    private static Map<String, List<Integer>> algoritmo(Map<Integer, List<String>> chunkMap) {
        // Extract the list of IP addresses from the chunkMap
        List<String> ipAddresses = chunkMap.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());

        // Initialize a map to store the load for each IP
        Map<String, Integer> ipLoad = new HashMap<>();
        for (String ipAddress : ipAddresses) {
            ipLoad.put(ipAddress, 0);
        }

        // Sort the IP addresses by load in ascending order
        ipAddresses.sort(Comparator.comparing(ipLoad::get));

        // Initialize the result map
        Map<String, List<Integer>> balancedChunks = new HashMap<>();
        for (String ipAddress : ipAddresses) {
            balancedChunks.put(ipAddress, new ArrayList<>());
        }

        for (Map.Entry<Integer, List<String>> entry : chunkMap.entrySet()) {
            int chunkNumber = entry.getKey();
            List<String> ipsWithChunk = entry.getValue();

            // Find the IP with the lowest load
            String minLoadIp = ipAddresses.get(0);

            // Assign the chunk to the IP with the lowest load
            balancedChunks.get(minLoadIp).add(chunkNumber);
            ipLoad.put(minLoadIp, ipLoad.get(minLoadIp) + 1);

            // Update the sorted IP addresses list
            ipAddresses.sort(Comparator.comparing(ipLoad::get));
        }

        return balancedChunks;
    }
}
