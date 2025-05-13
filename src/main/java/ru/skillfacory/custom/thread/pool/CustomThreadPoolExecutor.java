package ru.skillfacory.custom.thread.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPoolExecutor implements CustomExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CustomThreadPoolExecutor.class);

    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final int queueSize;
    private final int minSpareThreads;
    private final ThreadFactory threadFactory;
    private final RejectedExecutionHandler rejectedExecutionHandler;
    private final List<Worker> workers = new CopyOnWriteArrayList<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);
    private final AtomicInteger workerId = new AtomicInteger(1);
    private volatile boolean isShutdown = false;
    private volatile boolean isTerminated = false;

    public CustomThreadPoolExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime,
                                    int queueSize, int minSpareThreads, ThreadFactory threadFactory,
                                    RejectedExecutionHandler rejectedExecutionHandler) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;
        this.threadFactory = threadFactory;
        this.rejectedExecutionHandler = rejectedExecutionHandler;

        logger.info("[Pool] Initializing thread pool: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueSize);

        for (int i = 0; i < corePoolSize; i++) {
            addWorker();
        }
    }

    private synchronized void addWorker() {
        if (workers.size() >= maxPoolSize) {
            logger.warn("[Pool] Cannot create new worker: reached max pool size {}", maxPoolSize);
            return;
        }
        Worker worker = new Worker(workerId.getAndIncrement(), this);
        workers.add(worker);
        worker.start();
        logger.debug("[Pool] Added new worker: {}", worker.getId());
    }

    synchronized void removeWorker(Worker worker) {
        if (workers.remove(worker)) {
            logger.debug("[Pool] Removed worker: {}", worker.getId());
        }
    }

    public int getPoolSize() {
        return workers.size();
    }

    public int getQueueSize() {
        return queueSize;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            logger.warn("[Pool] Rejecting task {}: pool is shutting down", command);
            rejectedExecutionHandler.rejectedExecution(command, null);
            return;
        }

        logger.debug("[Pool] Received task: {}", command);
        List<Worker> currentWorkers = new ArrayList<>(workers);

        if (!currentWorkers.isEmpty()) {
            int startIdx = roundRobinIndex.getAndUpdate(i -> (i + 1) % currentWorkers.size());
            for (int i = 0; i < currentWorkers.size(); i++) {
                Worker worker = currentWorkers.get((startIdx + i) % currentWorkers.size());
                if (worker.getQueue().offer(command)) {
                    logger.info("[Pool] Task {} accepted to worker queue #{}", command, worker.getId());
                    ensureMinSpareThreads();
                    return;
                }
            }
        }

        synchronized (this) {
            if (workers.size() < maxPoolSize) {
                Worker newWorker = new Worker(workerId.getAndIncrement(), this);
                if (newWorker.getQueue().offer(command)) {
                    workers.add(newWorker);
                    newWorker.start();
                    logger.info("[Pool] Task {} accepted to new worker queue #{}", command, newWorker.getId());
                    ensureMinSpareThreads();
                    return;
                }
            }
        }

        logger.error("[Pool] All queues full, rejecting task: {}", command);
        rejectedExecutionHandler.rejectedExecution(command, null);
    }

    private void ensureMinSpareThreads() {
        long idleCount = workers.stream().filter(Worker::isIdle).count();
        logger.debug("[Pool] Checking min spare threads: current idle={}, required={}",
                idleCount, minSpareThreads);

        while (idleCount < minSpareThreads && workers.size() < maxPoolSize) {
            logger.info("[Pool] Creating spare worker (minSpareThreads={})", minSpareThreads);
            addWorker();
            idleCount++;
        }
    }

    @Override
    public void shutdown() {
        logger.info("[Pool] Initiating graceful shutdown");
        isShutdown = true;
        workers.forEach(worker -> {
            logger.debug("[Pool] Shutting down worker: {}", worker.getId());
            worker.shutdown();
        });
    }

    @Override
    public void shutdownNow() {
        logger.warn("[Pool] Forcing immediate shutdown");
        isShutdown = true;
        isTerminated = true;
        workers.forEach(worker -> {
            logger.debug("[Pool] Force shutting down worker: {}", worker.getId());
            worker.shutdown();
        });
        workers.clear();
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> future = new FutureTask<>(callable);
        execute(future);
        return future;
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public boolean isTerminated() {
        return isTerminated;
    }
}