package ru.skillfactory.custom.thread.pool;

@FunctionalInterface
public interface CustomRejectedExecutionHandler {
    void rejectedExecution(Runnable task, CustomThreadPool executor);
}

