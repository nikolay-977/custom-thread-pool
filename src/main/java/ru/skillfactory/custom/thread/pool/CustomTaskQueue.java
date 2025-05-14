package ru.skillfactory.custom.thread.pool;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class CustomTaskQueue implements BlockingQueue<Runnable> {
    private final BlockingQueue<Runnable> queue;

    public CustomTaskQueue(BlockingQueue<Runnable> queue) {
        this.queue = queue;
    }

    @Override
    public boolean add(Runnable runnable) {
        return queue.add(runnable);
    }

    @Override
    public boolean offer(Runnable task) {
        boolean result = queue.offer(task);
        if (result) {
            System.out.println("Task added to queue: " + task);
        } else {
            System.out.println("Failed to add task to queue: " + task);
        }
        return result;
    }

    @Override
    public Runnable remove() {
        return queue.remove();
    }

    @Override
    public void put(Runnable runnable) throws InterruptedException {
        queue.put(runnable);
    }

    @Override
    public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
        return queue.offer(runnable, timeout, unit);
    }

    @Override
    public Runnable poll() {
        Runnable task = queue.poll();
        if (task != null) {
            System.out.println("Task retrieved from queue: " + task);
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
        return queue.take();
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll();
    }

    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Runnable> c) {
        return queue.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public void clear() {
        queue.clear();
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
        return queue.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super Runnable> c, int maxElements) {
        return queue.drainTo(c, maxElements);
    }
}
