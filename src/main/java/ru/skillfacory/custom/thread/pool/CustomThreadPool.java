package ru.skillfacory.custom.thread.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPool implements CustomExecutor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int minSpareThreads;

    private final BlockingQueue<Runnable> globalQueue;
    private final List<Worker> workers;
    private final ThreadFactory threadFactory;
    private final RejectedExecutionHandler rejectionHandler;

    private volatile boolean isShutdown = false;
    private final AtomicInteger activeWorkers = new AtomicInteger(0);

    public CustomThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTime,
                            TimeUnit timeUnit, int queueSize, int minSpareThreads) {
        this(corePoolSize, maxPoolSize, keepAliveTime, timeUnit, queueSize,
                minSpareThreads, new CustomThreadFactory(), new CustomRejectionHandler());
    }

    public CustomThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTime,
                            TimeUnit timeUnit, int queueSize, int minSpareThreads,
                            ThreadFactory threadFactory, RejectedExecutionHandler rejectionHandler) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.minSpareThreads = minSpareThreads;
        this.threadFactory = threadFactory;
        this.rejectionHandler = rejectionHandler;

        this.globalQueue = new LinkedBlockingQueue<>(queueSize);
        this.workers = new ArrayList<>(maxPoolSize);

        // Инициализация core потоков
        for (int i = 0; i < corePoolSize; i++) {
            addWorker();
        }
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }

        // Проверяем минимальное количество резервных потоков
        checkSpareThreads();

        // Пробуем добавить задачу в очередь
        if (!globalQueue.offer(command)) {
            // Если очередь полная, пробуем добавить новый поток
            if (activeWorkers.get() < maxPoolSize) {
                addWorker();
                if (!globalQueue.offer(command)) {
                    // Если после добавления потока очередь все еще полная, применяем политику отказа
                    rejectionHandler.rejectedExecution(command, new ThreadPoolExecutor(
                            corePoolSize, maxPoolSize, keepAliveTime, timeUnit,
                            new LinkedBlockingQueue<>()));
                }
            } else {
                rejectionHandler.rejectedExecution(command, new ThreadPoolExecutor(
                        corePoolSize, maxPoolSize, keepAliveTime, timeUnit,
                        new LinkedBlockingQueue<>()));
            }
        } else {
            System.out.println("[Pool] Task accepted into queue: <" + command +">");
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> future = new FutureTask<>(callable);
        execute(future);
        return future;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        System.out.println("[Pool] Shutting down...");
        synchronized (workers) {
            for (Worker worker : workers) {
                worker.interruptIfIdle();
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        isShutdown = true;
        List<Runnable> remainingTasks = new ArrayList<>();
        synchronized (workers) {
            globalQueue.drainTo(remainingTasks);
            for (Worker worker : workers) {
                worker.interruptNow();
            }
        }
        return remainingTasks;
    }

    private void addWorker() {
        synchronized (workers) {
            if (workers.size() >= maxPoolSize || isShutdown) {
                return;
            }
            Worker worker = new Worker();
            workers.add(worker);
            activeWorkers.incrementAndGet();
            worker.thread.start();
        }
    }

    private void checkSpareThreads() {
        if (isShutdown) return;

        int availableThreads = activeWorkers.get() - globalQueue.size();
        if (availableThreads < minSpareThreads && activeWorkers.get() < maxPoolSize) {
            addWorker();
        }
    }

    private void workerTerminated(Worker worker) {
        synchronized (workers) {
            workers.remove(worker);
            activeWorkers.decrementAndGet();
            if (!isShutdown && workers.size() < corePoolSize) {
                addWorker(); // Поддерживаем минимальное количество потоков
            }
        }
    }

    private class Worker implements Runnable {
        final Thread thread;
        volatile boolean running = true;
        volatile boolean working = false;

        Worker() {
            this.thread = threadFactory.newThread(this);
        }

        @Override
        public void run() {
            try {
                while (running && !Thread.currentThread().isInterrupted()) {
                    working = false;
                    Runnable task = null;

                    try {
                        task = globalQueue.poll(keepAliveTime, timeUnit);
                    } catch (InterruptedException e) {
                        // Прерывание при shutdown/shutdownNow
                        Thread.currentThread().interrupt();
                        break;
                    }

                    if (task != null) {
                        working = true;
                        System.out.println("[Worker] " + thread.getName() + " executes <" + task.getClass().getSimpleName() + ">");
                        try {
                            task.run();
                        } catch (Exception e) {
                            System.out.println("[Worker] " + thread.getName() + " encountered exception executing task: " + e);
                        }
                    } else if (activeWorkers.get() > corePoolSize) {
                        // Idle timeout для не-core потоков
                        System.out.println("[Worker] " + thread.getName() + " idle timeout, stopping.");
                        break;
                    }
                }
            } finally {
                System.out.println("[Worker] " + thread.getName() + " terminated.");
                workerTerminated(this);
            }
        }

        void interruptIfIdle() {
            if (!working) {
                thread.interrupt();
                running = false;
            }
        }

        void interruptNow() {
            thread.interrupt();
            running = false;
        }
    }
}