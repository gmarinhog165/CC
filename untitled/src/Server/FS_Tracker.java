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
    private Map<String, Map<String, List<Integer>>> catalogo_chunks = new HashMap<>();

    // -> Map<Integer, SHA-1>
    private ReentrantReadWriteLock catalogo = new ReentrantReadWriteLock();
    Lock writel = catalogo.writeLock();
    Lock readl = catalogo.readLock();

    public FS_Tracker(){
        this.catalogo_chunks = new HashMap<>();
    }

    public Map<String, List<Integer>> getInfoFile(String file){
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
        int nchunks = chunk.getNum();
        try{
            this.writel.lock();
            // caso o file seja repetido
            if(this.catalogo_chunks.containsKey(name)){
                Map<String, List<Integer>> tmp = this.catalogo_chunks.get(name);
                for(int i = 1; i <= nchunks; i++){
                    // caso já haja algum Node com este chunk
                    if(tmp.containsKey(ip)){
                        List<Integer> tmp2 = tmp.get(ip);
                        tmp2.add(i);
                    }
                    // caso nao haja nenhum node com este chunk
                    else {
                        List<Integer> tmp2 = new ArrayList<>();
                        tmp2.add(i);
                        tmp.put(ip, tmp2);
                    }
                }
            }
            // caso o file seja novo
            else{
                Map<String, List<Integer>> tmp2 = new HashMap<>();
                List<Integer> tmp3 = new ArrayList<>();
                for(int i = 1; i <= nchunks; i++){
                    tmp3.add(i);
                }
                tmp2.put(ip,tmp3);
                this.catalogo_chunks.put(name, tmp2);
                System.out.println();
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
