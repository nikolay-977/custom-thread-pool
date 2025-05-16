package ru.skillfactory.custom.thread.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class CustomThreadPoolAdapter implements ExecutorService {
    private static final Logger logger = LoggerFactory.getLogger(CustomThreadPoolAdapter.class);
    private final CustomThreadPool customThreadPool;

    public CustomThreadPoolAdapter(CustomThreadPool customThreadPool) {
        this.customThreadPool = customThreadPool;
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down the custom thread pool");
        customThreadPool.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        logger.info("Shutting down the custom thread pool immediately");
        return customThreadPool.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        boolean shutdown = customThreadPool.isShutdown();
        logger.info("Is the custom thread pool shutdown? {}", shutdown);
        return shutdown;
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
        logger.info("Submitting runnable task: {}", task);
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
        logger.info("Awaiting termination of the custom thread pool for {} {}", timeout, unit);
        return customThreadPool.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        logger.info("Submitting callable task: {}", task);
        return customThreadPool.submit(task);
    }

    @Override
    public void execute(Runnable command) {
        logger.info("Executing command: {}", command);
        customThreadPool.execute(command);
    }
}
