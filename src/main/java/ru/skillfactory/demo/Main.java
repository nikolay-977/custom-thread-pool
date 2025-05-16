package ru.skillfactory.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.skillfactory.custom.thread.pool.CustomRejectedExecutionHandler;
import ru.skillfactory.custom.thread.pool.CustomThreadPool;
import ru.skillfactory.custom.thread.pool.RejectPolicy;
import ru.skillfactory.custom.thread.pool.RetryPolicy;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final AtomicInteger completedTasks = new AtomicInteger(0);
    private static final AtomicInteger rejectedTasks = new AtomicInteger(0);
    private static final AtomicInteger interruptedTasks = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        // Демонстрация обработки ситуации, когда поступает слишком много задач (задачи отклоняются)
        demo(new RejectPolicy(), "MyPool-with-rejected-tasks");

        // Демонстрация обработки ситуации, когда поступает слишком много задач (задачи обрабатываются согласно политике)
        demo(new RetryPolicy(), "MyPool-with-retried-tasks");
    }

    private static void demo(CustomRejectedExecutionHandler customRejectedExecutionHandler, String poolName) {
        logger.info("Starting demo with {} policy", customRejectedExecutionHandler.getClass().getSimpleName());

        // Создаем пул потоков
        CustomThreadPool pool = new CustomThreadPool(
                2, 4, 5, TimeUnit.SECONDS, 5, 1, customRejectedExecutionHandler, poolName);

        // Запускаем задачи
        for (int i = 0; i < 40; i++) {
            int taskNumber = i;
            try {
                pool.execute(() -> {
                    try {
                        logger.debug("Executing task {} on thread {}", taskNumber, Thread.currentThread().getName());
                        Thread.sleep(1000);
                        completedTasks.incrementAndGet();
                        logger.debug("Task {} completed on thread {}", taskNumber, Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        interruptedTasks.incrementAndGet();
                        logger.warn("Task {} interrupted", taskNumber);
                    }
                });
            } catch (RejectedExecutionException e) {
                rejectedTasks.incrementAndGet();
                logger.warn("Task {} rejected from pool", taskNumber);
            }
        }

        try {
            logger.info("Waiting for tasks to complete...");
            Thread.sleep(10000);

            logger.info("Initiating pool shutdown...");
            pool.shutdown();

            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Forcing shutdown because tasks did not finish in time");
                List<Runnable> tasks = pool.shutdownNow();
                logger.warn("{} tasks were not executed", tasks.size());
            } else {
                logger.info("All tasks completed successfully");
            }
        } catch (InterruptedException e) {
            logger.error("Main thread interrupted while waiting for pool termination");
            List<Runnable> tasks = pool.shutdownNow();
            logger.warn("{} tasks interrupted during shutdown", tasks.size());
            Thread.currentThread().interrupt();
        }

        logger.info("Demo finished for {}", poolName);
        logger.info("Statistics - Completed: {}, Rejected: {}, Interrupted: {}",
                completedTasks.get(), rejectedTasks.get(), interruptedTasks.get());
    }
}