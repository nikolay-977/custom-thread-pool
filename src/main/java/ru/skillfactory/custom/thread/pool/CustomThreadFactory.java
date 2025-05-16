package ru.skillfactory.custom.thread.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Фабрика для создания потоков, которая будет присваивать потокам уникальные имена
 * и логировать события их создания и завершения.
 */
public class CustomThreadFactory implements ThreadFactory {
    private static final Logger logger = LoggerFactory.getLogger(CustomThreadFactory.class);
    private final AtomicInteger threadCounter = new AtomicInteger(0);
    private final String poolName;

    public CustomThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = poolName + "-Thread-" + threadCounter.incrementAndGet();
        logger.info("Creating new thread: {}", threadName);
        return new Thread(r, threadName) {
            @Override
            public void run() {
                try {
                    super.run();
                } finally {
                    logger.info("Thread {} terminated", getName());
                }
            }
        };
    }
}
