package ru.skillfactory.custom.thread.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;

/**
 * Политика обработки отклоненных задач - пробуем повторно выполнить в текущем потоке
 * если не получается выполнить - отклоняем окончательно
 */
public class RetryPolicy implements CustomRejectedExecutionHandler {
    private static final Logger logger = LoggerFactory.getLogger(RetryPolicy.class);

    @Override
    public void rejectedExecution(Runnable task, CustomThreadPool executor) {
        // 1. Попробовать выполнить в текущем потоке
        if (!executor.isShutdown()) {
            logger.info("Retrying task execution in the current thread: {}", task);
            task.run();
            return;
        }

        // 2. Если ничего не получилось - логируем
        logger.error("Task finally rejected after retries: {}", task);
        throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + executor.getClass().getSimpleName());
    }
}
