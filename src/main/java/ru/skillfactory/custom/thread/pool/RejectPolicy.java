package ru.skillfactory.custom.thread.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Политика обработки отклоненных задач - просто отклоняет задачи с уведомлением
 */
public class RejectPolicy implements CustomRejectedExecutionHandler {
    private static final Logger logger = LoggerFactory.getLogger(RejectPolicy.class);
    private final AtomicInteger rejectedCount = new AtomicInteger();

    @Override
    public void rejectedExecution(Runnable task, CustomThreadPool executor) {
        rejectedCount.incrementAndGet();

        String errorMsg = String.format("Task %s rejected from %s",
                task.toString(), executor.getClass().getSimpleName());

        logger.warn("Task rejected - {}", errorMsg);
        logger.debug("Rejected task details: {}, pool status: {}",
                task, executor.getPoolStatus());

        throw new RejectedExecutionException(errorMsg);
    }
}