package ru.skillfacory.custom.thread.pool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

class CustomThreadFactory implements ThreadFactory {
    private static final Logger logger = Logger.getLogger(CustomThreadFactory.class.getName());
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public CustomThreadFactory() {
        this("CustomThreadPool-Worker-");
    }

    public CustomThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());

        // Настройки для оптимальной работы на M1
        if (System.getProperty("os.arch").equals("aarch64")) {
            t.setPriority(Thread.NORM_PRIORITY);
        } else {
            t.setPriority(Thread.NORM_PRIORITY);
        }

        t.setDaemon(false);
        logger.fine("Created new thread: " + t.getName());
        return t;
    }
}