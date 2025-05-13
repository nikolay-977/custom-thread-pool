package ru.skillfacory.custom.thread.pool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class CustomThreadFactory implements ThreadFactory {
    private final String namePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public CustomThreadFactory() {
        this("MyPool-worker-");
    }

    public CustomThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = namePrefix + threadNumber.getAndIncrement();
        Thread thread = new Thread(r, threadName);
        System.out.println("[ThreadFactory] Creating new thread: " + threadName);
        thread.setUncaughtExceptionHandler((t, e) ->
                System.out.println("[Worker] " + t.getName() + " encountered exception: " + e));
        return thread;
    }
}