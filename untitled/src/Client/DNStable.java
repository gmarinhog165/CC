package Client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DNStable {
    private Map<String, String> dns;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock writel = lock.writeLock();
    private Lock readl = lock.readLock();
    private List<String> hosts = new ArrayList<>();
    private ReentrantReadWriteLock lhosts = new ReentrantReadWriteLock();
    private Lock writel2 = lhosts.writeLock();
    private Lock readl2 = lhosts.readLock();


    public DNStable(){
        this.dns = new HashMap<>();
    }


    public boolean containsKey(String key){
        this.readl.lock();
        try{
            return this.dns.containsKey(key);
        } finally {
            this.readl.unlock();
        }
    }

    public String getIP(String key) {
        this.readl.lock();
        try{
            return this.dns.get(key);
        } finally {
            this.readl.unlock();
        }
    }

    public void insertIP(String hostname, String ip){
        this.writel.lock();
        this.writel2.lock();
        try{
            if(!this.dns.containsKey(hostname)){
                this.dns.put(hostname, ip);
                this.hosts.add(hostname);
            }

        } finally {
            System.out.println(this.dns);
            this.writel.unlock();
            this.writel2.unlock();
        }
    }

    public List<String> getHosts(){
        this.readl2.lock();
        try{
            List<String> tmp = List.copyOf(this.hosts);
            return tmp;
        } finally {
            this.readl2.unlock();
        }
    }

}
