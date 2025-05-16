package ru.skillfactory.custom.thread.pool;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

// Обработка отказов
// Просто отклоняем
public class RejectPolicy implements CustomRejectedExecutionHandler {
    private final AtomicInteger rejectedCount = new AtomicInteger();

    @Override
    public void rejectedExecution(Runnable task, CustomThreadPool executor) {
        System.out.println("Task finally rejected after retries");
        throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + executor.getClass().getSimpleName());
    }

    public int getRejectedCount() {
        return rejectedCount.get();
    }
}
