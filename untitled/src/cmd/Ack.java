package cmd;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ack {
    private Lock ackLock = new ReentrantLock();
    private Condition ackReceived = ackLock.newCondition();
    private boolean isAcknowledged = false;

    public void waitForAck() throws InterruptedException {
        ackLock.lock();
        try {
            while (!isAcknowledged) {
                ackReceived.await();
            }
            isAcknowledged = false; // Reset the acknowledgment state
        } finally {
            ackLock.unlock();
        }
    }

    public void sendAck() {
        ackLock.lock();
        try {
            isAcknowledged = true;
            ackReceived.signalAll();
        } finally {
            ackLock.unlock();
        }
    }
}
