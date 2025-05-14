package ru.skillfactory.custom.thread.pool;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public interface CustomExecutor extends Executor {
    void execute(Runnable command);

    <T> Future<T> submit(Callable<T> callable);

    void shutdown();

    List<Runnable> shutdownNow();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}