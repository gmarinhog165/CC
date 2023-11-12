package Client;

import cmd.Chunk;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TaskManager implements Runnable{
    private Socket socket;
    private List<Chunk> chunksDoMap;

    public TaskManager(Socket socket, List<Chunk> um){
        this.socket = socket;
        this.chunksDoMap = um;
    }

    @Override
    public void run() {
        try {
            messageManager(this.chunksDoMap);
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
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
            Map<Integer, List<String>> catalogo = deserializeMap(outputStream.toByteArray());
            // length do ultimo chunk
            int lenlastchunk = Integer.parseInt(catalogo.remove(-1).get(0));
            // número do último chunk
            int lastchunk = catalogo.keySet().stream()
                    .max(Integer::compare)
                    .orElse(null);

            Map<String, List<Integer>> locs = algoritmo(catalogo);

            ExecutorService executor = Executors.newFixedThreadPool(5);
            for(Map.Entry<String, List<Integer>> d : locs.entrySet()){
                String ip = d.getKey();
                List<Integer> chunks = d.getValue();
                for(int b : chunks){
                    Runnable worker;
                    if(b == lastchunk){
                        worker = new NodeSend(socket, ip, b, lenlastchunk);
                        System.out.println(lenlastchunk);
                    } else {
                        worker = new NodeSend(socket, ip, b, 986);
                    }
                    executor.execute(worker);

                }
                executor.shutdown();
                while (!executor.isTerminated());
                System.out.println("Finished all threads");
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
