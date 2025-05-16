package ru.skillfactory.custom.thread.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class CustomTaskQueue implements BlockingQueue<Runnable> {
    private static final Logger logger = LoggerFactory.getLogger(CustomTaskQueue.class);
    private final BlockingQueue<Runnable> queue;

    public CustomTaskQueue(BlockingQueue<Runnable> queue) {
        this.queue = queue;
    }

    @Override
    public boolean add(Runnable runnable) {
        boolean result = queue.add(runnable);
        logger.info("Task added: {}", runnable);
        return result;
    }

    @Override
    public boolean offer(Runnable task) {
        boolean result = queue.offer(task);
        if (result) {
            logger.info("Task added to queue: {}", task);
        } else {
            logger.warn("Failed to add task to queue: {}", task);
        }
        return result;
    }

    @Override
    public Runnable remove() {
        Runnable task = queue.remove();
        logger.info("Task removed: {}", task);
        return task;
    }

    @Override
    public void put(Runnable runnable) throws InterruptedException {
        queue.put(runnable);
        logger.info("Task put in queue: {}", runnable);
    }

    @Override
    public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
        boolean result = queue.offer(runnable, timeout, unit);
        if (result) {
            logger.info("Task added to queue with timeout: {}", runnable);
        } else {
            logger.warn("Failed to add task to queue with timeout: {}", runnable);
        }
        return result;
    }

    @Override
    public Runnable poll() {
        Runnable task = queue.poll();
        if (task != null) {
            logger.info("Task retrieved from queue: {}", task);
        }
        return task;
    }

    @Override
    public Runnable element() {
        return queue.element();
    }

    @Override
    public Runnable peek() {
        return queue.peek();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public Runnable take() throws InterruptedException {
        Runnable task = queue.take();
        logger.info("Task taken from queue: {}", task);
        return task;
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        boolean result = queue.remove(o);
        logger.info("Task removed from queue: {}", o);
        return result;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Runnable> c) {
        boolean result = queue.addAll(c);
        logger.info("Tasks added to queue: {}", c);
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = queue.removeAll(c);
        logger.info("Tasks removed from queue: {}", c);
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = queue.retainAll(c);
        logger.info("Tasks retained in queue: {}", c);
        return result;
    }

    @Override
    public void clear() {
        queue.clear();
        logger.info("Queue cleared");
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public Iterator<Runnable> iterator() {
        return queue.iterator();
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    @Override
    public int drainTo(Collection<? super Runnable> c) {
        int drained = queue.drainTo(c);
        logger.info("Drained {} tasks to collection", drained);
        return drained;
    }

    @Override
    public int drainTo(Collection<? super Runnable> c, int maxElements) {
        int drained = queue.drainTo(c, maxElements);
        logger.info("Drained {} tasks to collection with max elements {}", drained, maxElements);
        return drained;
    }
}
