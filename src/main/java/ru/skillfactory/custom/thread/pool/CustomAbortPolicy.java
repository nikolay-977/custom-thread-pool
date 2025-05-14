package ru.skillfactory.custom.thread.pool;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomAbortPolicy implements CustomRejectedExecutionHandler {
    private final AtomicInteger rejectedCount = new AtomicInteger();

    @Override
    public void rejectedExecution(Runnable task, CustomThreadPool executor) {
        rejectedCount.incrementAndGet();

        // 1. Попробовать добавить в другую очередь
        for (CustomTaskQueue queue : executor.getTaskQueues()) {
            if (queue.offer(task)) {
                return;
            }
        }

        // 2. Попробовать выполнить в текущем потоке
        if (!executor.isShutdown()) {
            task.run();
            return;
        }

        // 3. Если ничего не получилось - логируем
        System.out.println("Task finally rejected after retries");
        throw new RejectedExecutionException("Task " + task.toString() + " rejected from " + executor.getClass().getSimpleName());
    }

    public int getRejectedCount() {
        return rejectedCount.get();
    }
}
