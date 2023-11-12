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
    private Map<String, Map<Integer, List<String>>> catalogo_chunks;
    /**
     * hash que indica que files tem cada node
     */
    private Map<String, Set<String>> nodes_files;

    // -> Map<Integer, SHA-1>
    private ReentrantReadWriteLock catalogo = new ReentrantReadWriteLock();
    Lock writel = catalogo.writeLock();
    Lock readl = catalogo.readLock();

    public FS_Tracker(){
        this.catalogo_chunks = new HashMap<>();
        this.nodes_files = new HashMap<>();
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
        int chunks = chunk.getNum();
        int nchunks = calculateNumChunks(chunks);
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
                List<String> t = new ArrayList<>();
                t.add(String.valueOf(chunks%nchunks));
                tmp2.put(-1, t);
                this.catalogo_chunks.put(name, tmp2);
            }


            if(this.nodes_files.containsKey(ip)){
                Set<String> tt = this.nodes_files.get(ip);
                tt.add(name);
            }
            else{
                Set<String> tt2 = new HashSet<>();
                tt2.add(name);
                this.nodes_files.put(ip, tt2);
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
     * @param ip
     */
    public void deleteNode(String ip){
        Set<String> tmp = this.nodes_files.get(ip);
        for(String c : tmp){
            Map<Integer, List<String>> chunkIP = this.catalogo_chunks.get(c);
            for (List<String> ipList : chunkIP.values()) {
                ipList.removeIf(k -> k.equals(ip));
            }
            chunkIP.values().removeIf(List::isEmpty);

            if (chunkIP.isEmpty()) {
                this.catalogo_chunks.remove(c);
            }
        }
        this.nodes_files.remove(ip);
    }

    public static int calculateNumChunks(int totalBytes) {
        return (int) Math.ceil((double) totalBytes / 986);
    }

}
