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
    private Map<String, Map<Integer, Set<String>>> catalogo_chunks;
    /**
     * hash que indica que files tem cada node
     */
    private Map<String, Set<String>> nodes_files;

    /**
     *     Key : Nome File
     *     Value.Key: Chunk
     *     Value.Value: SHA-1
      */
    private Map<String, Map<Integer, byte[]>> SHA_1;
    private ReentrantReadWriteLock catalogo = new ReentrantReadWriteLock();
    Lock writel = catalogo.writeLock();
    Lock readl = catalogo.readLock();
    private List<String> nodes = new ArrayList<>();

    public FS_Tracker(){
        this.catalogo_chunks = new HashMap<>();
        this.nodes_files = new HashMap<>();
        this.SHA_1 = new HashMap<>();
    }

    public void insertSH1(List<Chunk> chunks, String path){
        if(!this.SHA_1.containsKey(path)){
            Map<Integer, byte[]> csha = new HashMap<>();
            for(Chunk c : chunks){
                byte[] value = c.getData();
                int key = c.getNum();
                csha.put(key,value);
            }
            this.SHA_1.put(path,csha);
        }
    }

    public Map<Integer, byte[]> getSHA1(String file){
        return this.SHA_1.get(file);
    }
    public Map<Integer, Set<String>> getInfoFile(String file){
        this.readl.lock();
        try{
            return this.catalogo_chunks.get(file);
        } finally {
            this.readl.unlock();
        }

    }

    /**
     * Método que para a Mensagem 2 adiciona a informação que o novo
     * Node que se conectou traz ao catálogo do Tracker.
     * Usa locks para garantir que as threads escrevem toda a informação quando o
     * tentam fazer concorrentemente.
     * @param chunk
     * @param hostName
     */
    public void writeFileOnHashMsg1(Chunk chunk, String hostName){
        String name = new String(chunk.getData());
        int chunks = chunk.getNum();
        int nchunks = calculateNumChunks(chunks);
        try{
            this.writel.lock();
            // caso o file seja repetido
            if(this.catalogo_chunks.containsKey(name)){
                Map<Integer, Set<String>> tmp = this.catalogo_chunks.get(name);
                for(int i = 1; i <= nchunks; i++){
                    // caso já haja algum Node com este chunk
                    if(tmp.containsKey(i)){
                        Set<String> tmp2 = tmp.get(i);
                        tmp2.add(hostName);
                    }
                    // caso nao haja nenhum node com este chunk
                    else {
                        Set<String> tmp2 = new HashSet<>();
                        tmp2.add(hostName);
                        tmp.put(i, tmp2);
                    }
                }
            }
            // caso o file seja novo
            else{
                Map<Integer, Set<String>> tmp2 = new HashMap<>();
                for(int i = 1; i <= nchunks; i++){
                    Set<String> tmp3 = new HashSet<>();
                    tmp3.add(hostName);
                    tmp2.put(i,tmp3);
                }
                Set<String> t = new HashSet<>();
                t.add(String.valueOf(chunks%986));
                tmp2.put(-1, t);
                this.catalogo_chunks.put(name, tmp2);
            }


            if(this.nodes_files.containsKey(hostName)){
                Set<String> tt = this.nodes_files.get(hostName);
                tt.add(name);
            }
            else{
                Set<String> tt2 = new HashSet<>();
                tt2.add(name);
                this.nodes_files.put(hostName, tt2);
                this.nodes.add(hostName);
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

    /**
     * Método que apaga toda a informação de um node quando este desconecta
     * @param hostName
     */
    public void deleteNode(String hostName){
        Set<String> tmp = this.nodes_files.get(hostName);
        for(String c : tmp){
            Map<Integer, Set<String>> chunkIP = this.catalogo_chunks.get(c);
            for (Set<String> ipList : chunkIP.values()) {
                ipList.removeIf(k -> k.equals(hostName));
            }
            chunkIP.values().removeIf(Set::isEmpty);

            if (chunkIP.isEmpty()) {
                this.catalogo_chunks.remove(c);
            }
        }
        this.nodes_files.remove(hostName);
    }

    public static int calculateNumChunks(int totalBytes) {
        return (int) Math.ceil((double) totalBytes / 986);
    }

    public void writeChunkOnHashMsg0(Chunk chunk, String hostName){
        this.writel.lock();
        try{
            String name = new String(chunk.getData());
            int numchunk = chunk.getNum();
            Map<Integer, Set<String>> tmp = this.catalogo_chunks.get(name);
            if(tmp.containsKey(numchunk)){
                Set<String> tmp2 = tmp.get(numchunk);
                tmp2.add(hostName);
            }
            // caso nao haja nenhum node com este chunk
            else {
                Set<String> tmp2 = new HashSet<>();
                tmp2.add(hostName);
                tmp.put(numchunk, tmp2);
            }
        } finally {
            this.writel.unlock();
        }
    }

    public List<String> getAllHosts(){
        return new ArrayList<>(this.nodes);
    }
}
