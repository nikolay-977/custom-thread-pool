package ru.skillfacory.custom.thread.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Worker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);

    private final BlockingQueue<Runnable> queue;
    private final CustomThreadPoolExecutor executor;
    private final Thread thread;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean isIdle = new AtomicBoolean(true);
    private final int id;

    public Worker(int id, CustomThreadPoolExecutor executor) {
        this.id = id;
        this.executor = executor;
        this.queue = new LinkedBlockingQueue<>(executor.getQueueSize());
        this.thread = executor.getThreadFactory().newThread(this);
    }

    public BlockingQueue<Runnable> getQueue() {
        return queue;
    }

    public boolean isIdle() {
        return isIdle.get();
    }

    public void start() {
        thread.start();
    }

    public void shutdown() {
        isRunning.set(false);
        thread.interrupt();
    }

    @Override
    public void run() {
        try {
            logger.info("[Worker] Thread {} started", thread.getName());

            while (isRunning.get() && !executor.isShutdown()) {
                isIdle.set(true);
                logger.debug("[Worker] Thread {} waiting for task", thread.getName());

                Runnable task = queue.poll(executor.getKeepAliveTime(), TimeUnit.SECONDS);
                isIdle.set(false);

                if (task != null) {
                    logger.info("[Worker] Thread {} executing {}", thread.getName(), task);
                    long startTime = System.currentTimeMillis();
                    task.run();
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("[Worker] Thread {} completed {} in {}ms",
                            thread.getName(), task, duration);
                } else {
                    logger.debug("[Worker] Thread {} received no task during timeout", thread.getName());
                    if (executor.getPoolSize() > executor.getCorePoolSize()) {
                        logger.info("[Worker] Thread {} terminating due to idle timeout", thread.getName());
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.warn("[Worker] Thread {} interrupted", thread.getName());
            Thread.currentThread().interrupt();
        } finally {
            executor.removeWorker(this);
            logger.info("[Worker] Thread {} terminated", thread.getName());
        }
    }

    public int getId() {
        return id;
    }
}