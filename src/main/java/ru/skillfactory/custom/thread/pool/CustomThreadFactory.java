package ru.skillfactory.custom.thread.pool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// Фабрику для создания потоков, которая будет присваивает потокам уникальные имена
// и логирует события их создания и завершения.
public class CustomThreadFactory implements ThreadFactory {
    private final AtomicInteger threadCounter = new AtomicInteger(0);
    private final String poolName;

    public CustomThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = poolName + "-Thread-" + threadCounter.incrementAndGet();
        System.out.println("Creating new thread: " + threadName);
        return new Thread(r, threadName) {
            @Override
            public void run() {
                try {
                    super.run();
                } finally {
                    System.out.println("Thread " + getName() + " terminated");
                }
            }
        };
    }
}
