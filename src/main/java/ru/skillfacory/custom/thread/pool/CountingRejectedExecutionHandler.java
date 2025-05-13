package ru.skillfacory.custom.thread.pool;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class CountingRejectedExecutionHandler implements RejectedExecutionHandler {
    private final AtomicInteger rejectedTasksCounter;

    public CountingRejectedExecutionHandler(AtomicInteger rejectedTasksCounter) {
        this.rejectedTasksCounter = rejectedTasksCounter;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        rejectedTasksCounter.incrementAndGet();
        System.out.println("[Rejected] Task " + r.toString() + " was rejected due to overload!");
        throw new RejectedExecutionException();
    }
}