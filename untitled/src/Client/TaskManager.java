package Client;

import cmd.Chunk;
import cmd.ConnectionTCP;
import cmd.FileManager;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class TaskManager implements Runnable{
    private ConnectionTCP con;
    private List<Chunk> chunksDoMap;
    private List<Chunk> shas1DoMap;
    private String file;
    private DNStable dns;
    private RTTMap rtts;

    public TaskManager(ConnectionTCP con, List<Chunk> um, String file, List<Chunk> shas, DNStable dns, RTTMap rtts){
        this.con = con;
        this.chunksDoMap = um;
        this.file = file;
        this.shas1DoMap = shas;
        this.dns = dns;
        this.rtts = rtts;
    }

    @Override
    public void run() {
        try {
            FileManager.createEmptyFile(FileManager.getFileName(this.file));
            messageManager(this.chunksDoMap, this.shas1DoMap);
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
    public void messageManager(List<Chunk> data, List<Chunk> shas) throws IOException, ClassNotFoundException, InterruptedException {
        // SHA-1
        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        for(Chunk c : shas){
            outputStream2.write(c.getData());
        }
        Map<Integer, byte[]> sha1s = deserializeShas(outputStream2.toByteArray());

        // Catalogo
        if(data.get(0).getMsg() == (byte) 5){
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // juntar todos os chunks de data correspondentes
            for(Chunk c : data){
                outputStream.write(c.getData());
            }
            // dar deserialize do byte[]
            Map<Integer, Set<String>> catalogo = deserializeMap(outputStream.toByteArray());
            // length do ultimo chunk
            int lenlastchunk = Integer.parseInt(new ArrayList<>(catalogo.remove(-1)).get(0));
            // número do último chunk
            int lastchunk = catalogo.keySet().stream()
                    .max(Integer::compare)
                    .orElse(null);

            Map<String, List<Integer>> locs = algoritmo(catalogo);

            try (ExecutorService executor = Executors.newFixedThreadPool(5)) {
                ReentrantLock lock = new ReentrantLock();
                for (Map.Entry<String, List<Integer>> d : locs.entrySet()) {
                    String hostName = d.getKey();
                    List<Integer> chunks = d.getValue();
                    for (int b : chunks) {
                        Runnable worker;
                        if (b == lastchunk) {
                            worker = new NodeSend(this.con, hostName, b, lenlastchunk, this.file, lock, sha1s.get(b), this.dns);
                        } else {
                            worker = new NodeSend(this.con, hostName, b, 986, this.file, lock, sha1s.get(b), this.dns);
                        }

                        executor.execute(worker);
                        byte[] asd = this.file.getBytes(StandardCharsets.UTF_8);
                        Chunk c = new Chunk(asd, asd.length, 0, true, (byte) 0, b);
                        con.send(c);
                        con.receive();

                    }
                }
                executor.shutdown();
                while (!executor.isTerminated()) ;
                System.out.println("File transfer is complete!");
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
    private Map<Integer, Set<String>> deserializeMap(byte[] serializedData) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedData);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (Map<Integer, Set<String>>) objectInputStream.readObject();
        }
    }

    private Map<Integer, byte[]> deserializeShas(byte[] serializedData) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedData);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (Map<Integer, byte[]>) objectInputStream.readObject();
        }
    }


    /**
     * Protótipo de algoritmo
     //* @param chunkMap
     * @return
     */
//    private static Map<String, List<Integer>> algoritmo(Map<Integer, Set<String>> chunkMap) {
//        // Extract the list of IP addresses from the chunkMap
//        List<String> ipAddresses = chunkMap.values().stream()
//                .flatMap(Collection::stream)
//                .distinct()
//                .collect(Collectors.toList());
//
//        // Initialize a map to store the load for each IP
//        Map<String, Integer> ipLoad = new HashMap<>();
//        for (String ipAddress : ipAddresses) {
//            ipLoad.put(ipAddress, 0);
//        }
//
//        // Sort the IP addresses by load in ascending order
//        ipAddresses.sort(Comparator.comparing(ipLoad::get));
//
//        // Initialize the result map
//        Map<String, List<Integer>> balancedChunks = new HashMap<>();
//        for (String ipAddress : ipAddresses) {
//            balancedChunks.put(ipAddress, new ArrayList<>());
//        }
//
//        for (Map.Entry<Integer, Set<String>> entry : chunkMap.entrySet()) {
//            int chunkNumber = entry.getKey();
//            Set<String> ipsWithChunk = entry.getValue();
//
//            // Find the IP with the lowest load
//            String minLoadIp = ipAddresses.get(0);
//
//            // Assign the chunk to the IP with the lowest load
//            balancedChunks.get(minLoadIp).add(chunkNumber);
//            ipLoad.put(minLoadIp, ipLoad.get(minLoadIp) + 1);
//
//            // Update the sorted IP addresses list
//            ipAddresses.sort(Comparator.comparing(ipLoad::get));
//        }
//
//        return balancedChunks;
//    }

    private Map<String, List<Integer>> algoritmo(Map<Integer, Set<String>> catalog) {
        List<String> ipAddresses = catalog.values().stream()
                .flatMap(Set::stream)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Long> rtt = this.rtts.getRtts();

        ipAddresses.sort(Comparator.comparingLong(rtt::get));

        Map<String, Double> normalizedRtt = new HashMap<>();
        for (String ipAddress : ipAddresses) {
            // Normaliza o RTT para um valor entre 0 e 1
            normalizedRtt.put(ipAddress, 1.0 / (1.0 + rtt.get(ipAddress)));
        }

        Map<String, Integer> ipLoad = new HashMap<>();
        for (String ipAddress : ipAddresses) {
            ipLoad.put(ipAddress, 0);
        }

        Map<String, List<Integer>> balancedChunks = new HashMap<>();
        for (String ipAddress : ipAddresses) {
            balancedChunks.put(ipAddress, new ArrayList<>());
        }

        for (Map.Entry<Integer, Set<String>> entry : catalog.entrySet()) {
            int chunkNumber = entry.getKey();
            Set<String> ipsWithChunk = entry.getValue();

            // Seleciona o host com a menor carga ponderada (considerando RTT)
            String minLoadAndRttIp = ipAddresses.stream()
                    .min(Comparator.comparingDouble(ip -> ipLoad.get(ip) + normalizedRtt.get(ip)))
                    .orElse(null);

            balancedChunks.get(minLoadAndRttIp).add(chunkNumber);
            ipLoad.put(minLoadAndRttIp, ipLoad.get(minLoadAndRttIp) + 1);
        }

        System.out.println(balancedChunks);
        return balancedChunks;
    }

}
