package ru.skillfactory.custom.thread.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Worker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);
    private static final AtomicLong workerCounter = new AtomicLong(0);

    private final CustomTaskQueue taskQueue;
    private final CustomThreadPool pool;
    private final long workerId;
    private Runnable firstTask;
    private volatile boolean isActive = true;
    private volatile Thread currentThread;
    private int completedTasks = 0;
    private volatile long lastActivityTime = System.currentTimeMillis();
    private volatile boolean isIdle = true;

    public Worker(Runnable firstTask, CustomTaskQueue taskQueue, CustomThreadPool pool) {
        this.firstTask = firstTask;
        this.taskQueue = taskQueue;
        this.pool = pool;
        this.workerId = workerCounter.incrementAndGet();
    }

    @Override
    public void run() {
        currentThread = Thread.currentThread();
        try {
            pool.beforeExecute(currentThread);
            lastActivityTime = System.currentTimeMillis();

            if (firstTask != null) {
                runTask(firstTask);
                firstTask = null;
            }

            // Обрабатывать задачи из закрепленной за ним очереди.
            while (isActive && !pool.isShutdown()) {
                Runnable task = getTask();
                if (task != null) {
                    runTask(task);
                } else if (shouldTerminate()) {
                    isActive = false;
                }
            }
        } catch (InterruptedException e) {
            handleInterruption(e);
        } finally {
            cleanup();
        }
    }

    private void runTask(Runnable task) {
        try {
            markBusy();
            logger.info("Worker {} is executing task: {}", workerId, task);
            task.run();
            completedTasks++;
            lastActivityTime = System.currentTimeMillis();
        } catch (RuntimeException e) {
            logger.error("Task execution failed in worker {} (pool: {})", workerId, pool.getClass().getSimpleName(), e);
            throw e;
        } finally {
            markIdle();
        }
    }

    private boolean shouldKeepAlive() {
        return pool.getTotalThreads() > pool.getCorePoolSize() ||
                (pool.getMinSpareThreads() > 0 && pool.getIdleThreads() > pool.getMinSpareThreads());
    }

    private boolean shouldTerminate() {
        return pool.isShutdown() ||
                (pool.getTotalThreads() > pool.getCorePoolSize()) ||
                (pool.getMinSpareThreads() > 0 && pool.getIdleThreads() > pool.getMinSpareThreads());
    }

    private void handleInterruption(InterruptedException e) {
        if (isActive && !pool.isShutdown()) {
            logger.warn("Worker {} was interrupted unexpectedly (pool: {})", workerId, pool.getClass().getSimpleName());
            Thread.currentThread().interrupt();
        }
    }

    private Runnable getTask() throws InterruptedException {
        try {
            markIdle();
            if (shouldKeepAlive()) {
                // Для не-core потоков используем poll с таймаутом
                Runnable task = taskQueue.poll(pool.getKeepAliveTime(), pool.getKeepAliveTimeUnit());
                if (task == null || pool.isShutdown()) {
                    isActive = false;
                    return null;
                }
                return task;
            } else {
                // Core потоки используют take() и работают постоянно
                if (pool.isShutdown()) {
                    isActive = false;
                    return null;
                }
                return taskQueue.take();
            }
        } catch (InterruptedException e) {
            if (pool.isShutdown()) {
                isActive = false;
            }
            throw e;
        }
    }

    private void cleanup() {
        try {
            pool.afterExecute(currentThread);
            pool.onWorkerExit(this);
            logger.info("Worker {} terminated. Completed tasks: {} (pool: {})", workerId, completedTasks, pool.getClass().getSimpleName());
        } finally {
            currentThread = null;
        }
    }

    private void markIdle() {
        if (!isIdle) {
            isIdle = true;
            pool.incrementIdleThreads();
            logger.info("Worker {} is now idle", workerId);
        }
    }

    private void markBusy() {
        if (isIdle) {
            isIdle = false;
            pool.decrementIdleThreads();
            logger.info("Worker {} is now busy", workerId);
        }
    }

    public void shutdown() {
        isActive = false;
        Thread thread = currentThread;
        if (thread != null) {
            thread.interrupt();
            logger.info("Worker {} has been interrupted", workerId);
        }
    }

    public void interruptNow() {
        isActive = false;
        Thread thread = currentThread;
        if (thread != null) {
            thread.interrupt();
            logger.info("Worker {} has been interrupted immediately", workerId);
        }
    }

    public long getIdleTime() {
        return isIdle ? System.currentTimeMillis() - lastActivityTime : 0;
    }

    @Override
    public String toString() {
        return String.format(
                "Worker[%d, active=%b, idle=%b, tasks=%d, idleTime=%dms]",
                workerId, isActive, isIdle, completedTasks, getIdleTime());
    }
}
