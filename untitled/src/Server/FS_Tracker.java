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

    public Map<Integer, List<String>> getInfoFile(String file){
        return this.catalogo_chunks.get(file);
    }

    /**
     * Método que para a Mensagem 2 adiciona a informação que o novo
     * Node que se conectou traz ao catálogo do Tracker.
     * Usa locks para garantir que as threads escrevem toda a informação quando o
     * tentam fazer concorrentemente.
     * @param chunk
     * @param ip
     */
    public void writeFileOnHashMsg1(Chunk chunk, String ip){
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
     * método para verificar se dado file existe no Tracker
     * @param file
     * @return
     */
    public boolean contains(String file){
        return this.catalogo_chunks.containsKey(file);
    }

}
