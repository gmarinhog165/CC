package Server;

import cmd.Chunk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class FS_Tracker {

    /**
     * Map com key sendo o nome do file
     * Value outro Map
     * * Cuja Key é o nº do chunk
     * * Valor é a lista dos IPs dos Nodes que tem o Chunk
     */
    private Map<String, Map<Integer, List<String>>> catalogo_chunks = new HashMap<>();
    /**
     * FUNÇÃO DE TESTE APENAS
     */
    public void addChunkAssignment() {
        int targetSizeInBytes = 2000;
        Map<String, Map<Integer, List<String>>> catalogo_chunks = new HashMap<>();
        int currentSizeInBytes = 0;
        String key = "IP1"; // The key you want to keep updating
        int iteration = 0;

        while (currentSizeInBytes < targetSizeInBytes) {
            Map<Integer, List<String>> tmp = catalogo_chunks.getOrDefault(key, new HashMap<>());
            List<String> asd = new ArrayList<>();

            // Generate a larger list (you can adjust the size as needed)
            for (int i = 0; i < 10; i++) {
                asd.add("Value " + i);
            }

            tmp.put(iteration, asd);

            catalogo_chunks.put(key, tmp);

            // Calculate the size of the map and its contents
            currentSizeInBytes = calculateMapSize(catalogo_chunks);

            if (currentSizeInBytes >= targetSizeInBytes) {
                break;
            }

            iteration++; // Increment the key or use another approach to control the size
        }
        this.catalogo_chunks = catalogo_chunks;
    }

    private int calculateMapSize(Map<String, Map<Integer, List<String>>> map) {
        // This method calculates the approximate size of the map and its contents in bytes
        // You can implement this method based on your specific needs

        // In a simplified example, you can count the number of entries and estimate the size
        // based on an average size of a string key and the List of strings
        int estimatedSize = 0;
        for (Map.Entry<String, Map<Integer, List<String>>> entry : map.entrySet()) {
            estimatedSize += entry.getKey().length(); // Key size
            for (List<String> strings : entry.getValue().values()) {
                estimatedSize += strings.size() * 12; // Average string size
            }
        }

        return estimatedSize;
    }

    // -> Map<Integer, SHA-1>
    private ReentrantReadWriteLock catalogo = new ReentrantReadWriteLock();
    Lock writel = catalogo.writeLock();
    Lock readl = catalogo.readLock();

    public FS_Tracker(){
        this.catalogo_chunks = new HashMap<>();
    }

    /**
     * Método que para a Mensagem 2 adiciona a informação que o novo
     * Node que se conectou traz ao catálogo do Tracker.
     * Usa locks para garantir que as threads escrevem toda a informação quando o
     * tentam fazer concorrentemente.
     * @param chunk
     * @param ip
     */
    public void writeFileOnHashMsg2(Chunk chunk, String ip){
        String name = new String(chunk.getData());
        int nchunks = chunk.getOffset();
        try{
            this.writel.lock();
            // caso o file seja repetido
            if(this.catalogo_chunks.containsKey(name)){
                Map<Integer, List<String>> tmp = this.catalogo_chunks.get(name);
                for(int i = 1; i <= nchunks; i++){
                    // caso já haja algum Node com este chunk
                    if(tmp.containsKey(i)){
                        List<String> tmp2 = tmp.get(i);
                        tmp2.add(ip);
                    }
                    // caso nao haja nenhum node com este chunk
                    else {
                        List<String> tmp2 = new ArrayList<>();
                        tmp2.add(ip);
                        tmp.put(i, tmp2);
                    }
                }
            }
            // caso o file seja novo
            else{
                Map<Integer, List<String>> tmp2 = new HashMap<>();
                for(int i = 1; i <= nchunks; i++){
                    List<String> tmp3 = new ArrayList<>();
                    tmp3.add(ip);
                    tmp2.put(i,tmp3);
                }
                this.catalogo_chunks.put(name, tmp2);
            }
        } finally {
            this.writel.unlock();
        }
    }

    /**
     * protótipo de algoritmo
     *
     * @param file
     * @return
     */
    public List<Chunk> algoritmo(String file){
        Map<String, List<Integer>> locs = new HashMap<>();
        try{
            this.readl.lock();
            Map<Integer, List<String>> locDoFile = this.catalogo_chunks.get(file);
            return (Chunk.fromByteArray(serializeMap(balanceChunks(locDoFile)), (byte) 2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally{
            this.readl.unlock();
        }
    }

    private static Map<String, List<Integer>> balanceChunks(Map<Integer, List<String>> chunkMap) {
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

    private byte[] serializeMap(Map<String, List<Integer>> map) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(map);
            return byteArrayOutputStream.toByteArray();
        }
    }

    public boolean contains(String file){
        return this.catalogo_chunks.containsKey(file);
    }

}
