package ru.skillfacory.custom.thread.pool;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final AtomicInteger completedTasks = new AtomicInteger(0);
    private static final AtomicInteger rejectedTasks = new AtomicInteger(0);

    public static void main(String[] args) {
        // Настройка логирования
        Logger.getLogger("").setLevel(Level.INFO);

        // Создание пула с оптимизацией для M1
        CustomExecutor pool = new CustomThreadPool(
                2,  // corePoolSize
                4,  // maxPoolSize
                5,  // keepAliveTime
                TimeUnit.SECONDS,
                5,  // queueSize
                1   // minSpareThreads
        );

        logger.info("Starting thread pool demonstration on " + System.getProperty("os.arch"));

        demo(pool);

        logger.info("\nFinal Statistics:");
        logger.info("Total completed tasks: " + completedTasks.get());
        logger.info("Total rejected tasks: " + rejectedTasks.get());
    }

    private static void demo(CustomExecutor pool) {
        logger.info("\nTesting normal operation...");
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            pool.execute(() -> {
                logger.info("Task " + taskId + " started");
                try {
                    Thread.sleep(1000);
                    completedTasks.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                logger.info("Task " + taskId + " completed");
            });
        }
        pool.shutdown();
    }
}