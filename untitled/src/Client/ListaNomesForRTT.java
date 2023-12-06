package Client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ListaNomesForRTT {
    private List<String> nomes;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock writel = lock.writeLock();
    private Lock readl = lock.readLock();

    public ListaNomesForRTT(List<String> nomes){
        this.nomes = nomes;
    }

    public void addNomes(String nome){
        this.writel.lock();
        try{
            this.nomes.add(nome);
        } finally {
            this.writel.unlock();
        }
    }

    public List<String> getNomes(){
        this.readl.lock();
        try{
            List<String> tmp = new ArrayList<>();
            for(String c:this.nomes){
                tmp.add(c);
            }
            return tmp;
        } finally {
            this.readl.unlock();
        }
    }

    public int getSize(){
        this.readl.lock();
        try{
            return this.nomes.size();
        } finally {
            this.readl.unlock();
        }
    }

    public String getElement(int i){
        this.readl.lock();
        try{
            String n = this.nomes.get(i);
            return n;
        } finally {
            this.readl.unlock();
        }
    }
}
