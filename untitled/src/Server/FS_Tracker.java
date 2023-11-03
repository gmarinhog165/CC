package Server;

import cmd.Chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
public class FS_Tracker {

    /**
     * Map com key sendo o nome do file
     * Value outro Map
     * * Cuja Key é o nº do chunk
     * * Valor é a lista dos IPs dos Nodes que tem o Chunk
     */
    private Map<String, Map<Integer, List<String>>> catalogo_chunks;
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

}
