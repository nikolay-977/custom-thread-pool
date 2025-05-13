package ru.skillfacory.custom.thread.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomThreadPool implements CustomExecutor {
    private static final Logger logger = Logger.getLogger(CustomThreadPool.class.getName());

    // Конфигурационные параметры
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int queueSize;
    private final int minSpareThreads;

    // Внутренние компоненты
    private final List<Worker> workers;
    private final List<BlockingQueue<Runnable>> queues;
    private final CustomThreadFactory threadFactory;
    private final CustomRejectionHandler rejectionHandler;

    // Состояние пула
    private volatile boolean isShutdown = false;
    private volatile boolean isTerminated = false;
    private final ReentrantLock mainLock = new ReentrantLock();
    private final AtomicInteger currentPoolSize = new AtomicInteger(0);
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger nextQueueIndex = new AtomicInteger(0);

    public CustomThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTime,
                            TimeUnit timeUnit, int queueSize, int minSpareThreads) {
        // Валидация параметров
        if (corePoolSize < 0 || maxPoolSize <= 0 || maxPoolSize < corePoolSize ||
                keepAliveTime < 0 || queueSize <= 0 || minSpareThreads < 0) {
            throw new IllegalArgumentException("Invalid thread pool parameters");
        }

        // Оптимизация для Apple M1 (меньше ядер, но более производительные)
        if (System.getProperty("os.arch").equals("aarch64")) {
            logger.info("Optimizing for Apple M1 architecture");
            corePoolSize = Math.min(corePoolSize, 8); // M1 обычно имеет 8 ядер
            maxPoolSize = Math.min(maxPoolSize, 16);  // Ограничение для предотвращения перегрузки
        }

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.queueSize = queueSize;
        this.minSpareThreads = minSpareThreads;

        this.workers = new ArrayList<>(maxPoolSize);
        this.queues = new ArrayList<>(maxPoolSize);
        this.threadFactory = new CustomThreadFactory();
        this.rejectionHandler = new CustomRejectionHandler();

        initializePool();
    }

    private void initializePool() {
        mainLock.lock();
        try {
            for (int i = 0; i < corePoolSize; i++) {
                createWorker();
            }
            logger.info("Thread pool initialized with " + corePoolSize + " core threads");
        } finally {
            mainLock.unlock();
        }
    }

    private void createWorker() {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(queueSize);
        queues.add(queue);
        Worker worker = new Worker(queue);
        workers.add(worker);
        Thread thread = threadFactory.newThread(worker);
        thread.start();
        currentPoolSize.incrementAndGet();
    }

    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }

        if (isShutdown) {
            rejectionHandler.rejectedExecution(task, null);
            return;
        }

        mainLock.lock();
        try {
            // Проверяем, нужно ли создавать новые потоки
            int activeCount = activeThreads.get();
            int currentSize = currentPoolSize.get();

            if (activeCount >= currentSize && currentSize < maxPoolSize) {
                createWorker();
                logger.fine("Created new worker thread. Current pool size: " + currentSize);
            }

            // Выбираем очередь для задачи
            BlockingQueue<Runnable> targetQueue = getTargetQueue();

            if (!targetQueue.offer(task)) {
                rejectionHandler.rejectedExecution(task, null);
            } else {
                logger.finest("Task submitted to queue " + queues.indexOf(targetQueue));
            }
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> future = new FutureTask<>(callable);
        execute(future);
        return future;
    }

    private BlockingQueue<Runnable> getTargetQueue() {
        // Round-robin с проверкой на пустые очереди
        int size = queues.size();
        if (size == 0) {
            throw new IllegalStateException("No worker queues available");
        }

        int index = nextQueueIndex.getAndIncrement() % size;
        return queues.get(index);
    }

    @Override
    public void shutdown() {
        mainLock.lock();
        try {
            isShutdown = true;
            logger.info("Thread pool shutdown initiated");
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void shutdownNow() {
        mainLock.lock();
        try {
            isShutdown = true;
            isTerminated = true;
            for (Worker worker : workers) {
                worker.interrupt();
            }
            logger.info("Thread pool immediate shutdown initiated");
        } finally {
            mainLock.unlock();
        }
    }

    private class Worker implements Runnable {
        private final BlockingQueue<Runnable> queue;
        private volatile boolean running = true;
        private Thread currentThread;

        public Worker(BlockingQueue<Runnable> queue) {
            this.queue = queue;
        }

        public void interrupt() {
            running = false;
            if (currentThread != null) {
                currentThread.interrupt();
            }
        }

        @Override
        public void run() {
            currentThread = Thread.currentThread();
            while (running && !isTerminated) {
                try {
                    Runnable task = queue.poll(keepAliveTime, timeUnit);
                    if (task != null) {
                        activeThreads.incrementAndGet();
                        try {
                            task.run();
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Task execution failed", e);
                        } finally {
                            activeThreads.decrementAndGet();
                        }
                    } else if (shouldTerminate()) {
                        break;
                    }
                } catch (InterruptedException e) {
                    if (!running || isTerminated) {
                        break;
                    }
                }
            }

            // Очистка ресурсов
            cleanupWorker();
        }

        private boolean shouldTerminate() {
            return currentPoolSize.get() > corePoolSize ||
                    (isShutdown && queue.isEmpty());
        }

        private void cleanupWorker() {
            currentPoolSize.decrementAndGet();
            workers.remove(this);
            queues.remove(queue);
            logger.fine("Worker thread terminated. Current pool size: " + currentPoolSize.get());

            if (isShutdown && workers.isEmpty()) {
                logger.info("All worker threads have terminated");
            }
        }
    }
}