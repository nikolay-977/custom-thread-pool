package ru.skillfactory.custom.thread.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPool implements CustomExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CustomThreadPool.class);
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int minSpareThreads;
    private final CustomRejectedExecutionHandler rejectedExecutionHandler;

    private final List<CustomTaskQueue> taskQueues;
    private final List<Worker> workers = new ArrayList<>();
    private final AtomicInteger totalThreads = new AtomicInteger(0);
    private final AtomicInteger idleThreads = new AtomicInteger(0);
    private final ThreadFactory threadFactory;
    private volatile boolean isShutdown = false;

    public CustomThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit,
                            int queueSize, int minSpareThreads,
                            CustomRejectedExecutionHandler rejectedExecutionHandler, String poolName) {
        if (corePoolSize < 1 || maxPoolSize < corePoolSize || keepAliveTime < 0 ||
                queueSize < 1 || minSpareThreads < 0 || minSpareThreads > corePoolSize) {
            throw new IllegalArgumentException("Invalid thread pool parameters");
        }

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.minSpareThreads = minSpareThreads;
        this.rejectedExecutionHandler = rejectedExecutionHandler;

        this.taskQueues = new ArrayList<>(corePoolSize);
        for (int i = 0; i < corePoolSize; i++) {
            taskQueues.add(new CustomTaskQueue(new LinkedBlockingQueue<>(queueSize)));
        }

        this.threadFactory = new CustomThreadFactory(poolName);

        // Инициализация core потоков
        for (int i = 0; i < corePoolSize; i++) {
            createWorker(null, taskQueues.get(i));
        }
    }

    @Override
    public void execute(Runnable task) {
        if (task == null) throw new NullPointerException();
        if (isShutdown) {
            logger.warn("Task rejected because the pool is shutdown: {}", task);
            rejectedExecutionHandler.rejectedExecution(task, this);
            return;
        }

        // Распределение задач
        // 1. Быстрая попытка добавить в случайную очередь
        CustomTaskQueue randomQueue = taskQueues.get(ThreadLocalRandom.current().nextInt(taskQueues.size()));
        if (randomQueue.offer(task)) {
            logger.info("Task added to random queue: {}", task);
            return;
        }

        // 2. Попытка добавить в наименее загруженную очередь
        CustomTaskQueue leastLoaded = findLeastLoadedQueue();
        if (leastLoaded != null && leastLoaded.offer(task)) {
            logger.info("Task added to least loaded queue: {}", task);
            return;
        }

        // 3. Попытка создать новый поток
        if (totalThreads.get() < maxPoolSize) {
            if (totalThreads.get() < corePoolSize ||
                    idleThreads.get() < minSpareThreads) {
                createWorker(task, leastLoaded != null ? leastLoaded : randomQueue);
                logger.info("Created new worker for task: {}", task);
                return;
            }
        }

        logger.warn("Task rejected due to max pool size reached: {}", task);
        rejectedExecutionHandler.rejectedExecution(task, this);
    }

    private CustomTaskQueue findLeastLoadedQueue() {
        CustomTaskQueue leastLoaded = null;
        int minSize = Integer.MAX_VALUE;

        for (CustomTaskQueue queue : taskQueues) {
            int size = queue.size();
            if (size < minSize) {
                minSize = size;
                leastLoaded = queue;
            }
        }
        return leastLoaded;
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        if (callable == null) throw new NullPointerException("Callable cannot be null");
        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        logger.info("Submitted callable task: {}", callable);
        return futureTask;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        logger.info("Shutting down the thread pool");
        synchronized (workers) {
            // Очищаем все очереди задач
            synchronized (taskQueues) {
                for (CustomTaskQueue queue : taskQueues) {
                    queue.clear();
                    logger.info("Cleared task queue: {}", queue);
                }
            }
            // Прерываем все worker'ы
            for (Worker worker : workers) {
                worker.shutdown();
                logger.info("Worker {} has been shut down", worker);
            }
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long endTime = System.nanoTime() + unit.toNanos(timeout);
        logger.info("Awaiting termination of the thread pool");
        synchronized (workers) {
            while (totalThreads.get() > 0) {
                long remaining = endTime - System.nanoTime();
                if (remaining <= 0) {
                    logger.warn("Timeout reached while awaiting termination");
                    return false;
                }
                workers.wait(Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(100)));
            }
            logger.info("All workers have terminated");
            return true;
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        isShutdown = true;
        List<Runnable> remainingTasks = new ArrayList<>();

        synchronized (taskQueues) {
            // 1. Собираем невыполненные задачи
            for (CustomTaskQueue queue : taskQueues) {
                queue.drainTo(remainingTasks);
                logger.info("Drained tasks from queue: {}", queue);
            }

            // 2. Прерываем все рабочие потоки
            synchronized (workers) {
                for (Worker worker : workers) {
                    worker.interruptNow();
                    logger.info("Worker {} has been interrupted", worker);
                }
            }
        }

        return remainingTasks;
    }

    public String getPoolStatus() {
        String status = String.format(
                "Threads: %d/%d (active/total), Idle: %d, Queue: %d",
                getTotalThreads() - getIdleThreads(),
                getTotalThreads(),
                getIdleThreads(),
                getCurrentQueueSize()
        );
        logger.info("Current pool status: {}", status);
        return status;
    }

    private int getCurrentQueueSize() {
        int size = 0;
        for (CustomTaskQueue queue : taskQueues) {
            size += queue.size();
        }
        return size;
    }

    private void createWorker(Runnable firstTask, CustomTaskQueue queue) {
        synchronized (workers) {
            if (totalThreads.get() >= maxPoolSize) {
                logger.warn("Cannot create new worker, max pool size reached");
                return; // Не создаем больше потоков чем максимум
            }
            Worker worker = new Worker(firstTask, queue, this);
            Thread thread = threadFactory.newThread(worker);
            workers.add(worker);
            totalThreads.incrementAndGet();
            idleThreads.incrementAndGet();
            thread.start();
            logger.info("Created new worker: {}", worker);
        }
    }

    void onWorkerExit(Worker worker) {
        synchronized (workers) {
            if (workers.remove(worker)) {
                totalThreads.decrementAndGet();
                idleThreads.decrementAndGet();
                logger.info("Worker {} has exited", worker);
            }
        }
        // Проверяем нужно ли создать новый worker для поддержания minSpareThreads
        if (!isShutdown && totalThreads.get() < minSpareThreads) {
            createWorker(null, taskQueues.get(0));
            logger.info("Created new worker to maintain minimum spare threads");
        }
    }

    void beforeExecute(Thread t) {
        idleThreads.decrementAndGet();
        logger.info("Worker {} is about to execute a task", t.getName());
    }

    void afterExecute(Thread t) {
        idleThreads.incrementAndGet();
        logger.info("Worker {} has finished executing a task", t.getName());
    }

    public void incrementIdleThreads() {
        idleThreads.incrementAndGet();
    }

    public void decrementIdleThreads() {
        idleThreads.decrementAndGet();
    }

    // Геттеры
    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMinSpareThreads() {
        return minSpareThreads;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public TimeUnit getKeepAliveTimeUnit() {
        return timeUnit;
    }

    public int getTotalThreads() {
        return totalThreads.get();
    }

    public int getIdleThreads() {
        return idleThreads.get();
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public List<CustomTaskQueue> getTaskQueues() {
        return taskQueues;
    }
}
