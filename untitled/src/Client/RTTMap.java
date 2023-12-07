package Client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RTTMap {
    private Map<String, Long> rtts;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Lock writel = lock.writeLock();
    private Lock readl = lock.readLock();

    public RTTMap(){
        this.rtts = new HashMap<>();
    }

    public void put(String key, Long value){
        this.writel.lock();
        try{
            this.rtts.put(key,value);
        } finally {
            this.writel.unlock();
        }
    }

    public Map<String, Long> getRtts(){
        this.readl.lock();
        try{
            return deepCloneMap(this.rtts);
        } finally {
            this.readl.unlock();
        }
    }

    private Map<String, Long> deepCloneMap(Map<String, Long> originalMap) {
        Map<String, Long> clonedMap = new HashMap<>();

        for (Map.Entry<String, Long> entry : originalMap.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();

            clonedMap.put(key, value);
        }

        return clonedMap;
    }
}
