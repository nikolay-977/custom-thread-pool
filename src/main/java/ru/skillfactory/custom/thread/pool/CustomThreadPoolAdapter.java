package ru.skillfactory.custom.thread.pool;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class CustomThreadPoolAdapter implements ExecutorService {
    private final CustomThreadPool customThreadPool;

    public CustomThreadPoolAdapter(CustomThreadPool customThreadPool) {
        this.customThreadPool = customThreadPool;
    }

    @Override
    public void shutdown() {
        customThreadPool.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return customThreadPool.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return customThreadPool.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        throw new UnsupportedOperationException("Method is not implemented");
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        throw new UnsupportedOperationException("Method is not implemented");
    }

    @Override
    public Future<?> submit(Runnable task) {
        throw new UnsupportedOperationException("Method is not implemented");
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new UnsupportedOperationException("Method is not implemented");
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Method is not implemented");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException("Method is not implemented");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Method is not implemented");
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return customThreadPool.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return submit(task);
    }

    @Override
    public void execute(Runnable command) {
        customThreadPool.execute(command);
    }
}
