package Client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DNStable {
    private Map<String, String> dns;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock writel = lock.writeLock();
    private Lock readl = lock.readLock();

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
        try{
            if(!this.dns.containsKey(hostname))
                this.dns.put(hostname, ip);
        } finally {
            System.out.println(this.dns);
            this.writel.unlock();
        }
    }


}
