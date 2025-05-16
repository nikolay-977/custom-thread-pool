package ru.skillfactory.custom.thread.pool;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

// Обработка отказов
// Пробуем повторно выполнить в текущем потоке
public class RetryPolicy implements CustomRejectedExecutionHandler {
    private final AtomicInteger rejectedCount = new AtomicInteger();
    private final AtomicInteger retryerCount = new AtomicInteger();

    @Override
    public void rejectedExecution(Runnable task, CustomThreadPool executor) {
        // 1. Попробовать выполнить в текущем потоке
        if (!executor.isShutdown()) {
            task.run();
            return;
        }

        // 2. Если ничего не получилось - логируем
        System.out.println("Task finally rejected after retries");
        throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + executor.getClass().getSimpleName());
    }

    public int getRetryerCount() {
        return retryerCount.get();
    }

    public int getRejectedCount() {
        return rejectedCount.get();
    }
}
