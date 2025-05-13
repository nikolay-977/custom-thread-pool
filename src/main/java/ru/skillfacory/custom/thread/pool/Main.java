package ru.skillfacory.custom.thread.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        CustomThreadFactory threadFactory = new CustomThreadFactory("MyPool");
        CustomRejectedExecutionHandler rejectionHandler = new CustomRejectedExecutionHandler();

        CustomThreadPoolExecutor executor = new CustomThreadPoolExecutor(
                2, 4, 5,
                2, 1, threadFactory, rejectionHandler
        );

        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            try {
                Runnable task = new TaskWrapper(
                        () -> {
                            logger.info("Task {} started", taskId);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                logger.warn("Task {} interrupted", taskId);
                                Thread.currentThread().interrupt();
                            }
                            logger.info("Task {} completed", taskId);
                        },
                        "Task-" + taskId,
                        "ID-" + taskId
                );

                executor.execute(task);
            } catch (RejectedExecutionException e) {
                logger.error("Task {} rejected: {}", taskId, e.getMessage());
            }
            Thread.sleep(200);
        }

        executor.shutdown();

        while (!executor.isTerminated()) {
            Thread.sleep(1000);
        }

        logger.info("All tasks completed, thread pool shutdown");
    }
}