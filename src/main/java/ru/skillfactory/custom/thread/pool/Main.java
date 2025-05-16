package ru.skillfactory.custom.thread.pool;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final AtomicInteger completedTasks = new AtomicInteger(0);
    private static final AtomicInteger rejectedTasks = new AtomicInteger(0);
    private static final AtomicInteger interruptedTasks = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        // Демонстрируется обработки ситуации, когда поступает слишком много задач (задачи отклоняются).
        demo(new RejectPolicy(), "MyPool-with-rejected-tasks");
        // Демонстрируется обработки ситуации, когда поступает слишком много задач (задачи обрабатываются согласно заданной политике).
        demo(new RetryPolicy(), "MyPool-with-retried-tasks");
    }

    private static void demo(CustomRejectedExecutionHandler customRejectedExecutionHandler, String poolName) {
        // Создаем пул потоков
        CustomThreadPool pool = new CustomThreadPool(
                2, 4, 5, TimeUnit.SECONDS, 5, 1, customRejectedExecutionHandler, poolName);

        // Запускаем задачи
        for (int i = 0; i < 40; i++) {
            int taskNumber = i;
            try {
                pool.execute(() -> {
                    try {
                        System.out.println("Executing task " + taskNumber + " on thread " + Thread.currentThread().getName());
                        Thread.sleep(1000);
                        completedTasks.incrementAndGet();
                        System.out.println("Task " + taskNumber + " completed on thread " + Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        interruptedTasks.incrementAndGet();
                        System.out.println("Task " + taskNumber + " interrupted");
                    }
                });
            } catch (RejectedExecutionException e) {
                rejectedTasks.incrementAndGet();
                System.out.println("Task " + taskNumber + " rejected from pool");
            }
        }

        try {
            Thread.sleep(10000);
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Forcing shutdown because tasks did not finish in time");
                List<Runnable> tasks = pool.shutdownNow();
                System.out.println("Forcing shutdown because tasks: " + tasks.size());
            } else {
                System.out.println("All tasks completed. ");
            }
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted while waiting for pool termination");
            List<Runnable> tasks = pool.shutdownNow();
            System.out.println("Interrupted tasks while waiting for pool termination: " + tasks.size());
        }

        System.out.println("Program finished");
        System.out.println("Completed tasks: " + completedTasks.get());
        System.out.println("Rejected tasks: " + rejectedTasks.get());
        System.out.println("Interrupted tasks: " + interruptedTasks.get());
    }
}
