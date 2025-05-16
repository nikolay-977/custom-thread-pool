package ru.skillfactory.demo;

import ru.skillfactory.custom.thread.pool.CustomThreadPool;
import ru.skillfactory.custom.thread.pool.CustomThreadPoolAdapter;
import ru.skillfactory.custom.thread.pool.RejectPolicy;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ComparePerformance {
    private static final int TASK_COUNT = 100;
    private static final int TASK_DURATION_MS = 10;
    private static final AtomicInteger rejectedTasks = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("Performance Comparison between CustomThreadPool and Standard Pools");
        System.out.println("================================================================");

        List<String> csvResults = new ArrayList<>();
        // Заголовки CSV
        csvResults.add("Configuration,PoolType,AvgTimeMs,MinTimeMs,MaxTimeMs,TasksCompleted,TotalTasks,SuccessRate%");

        // Тестируем разные конфигурации пулов
        testConfiguration(2, 4, 10, "Small Pool", csvResults);
        testConfiguration(4, 8, 20, "Medium Pool", csvResults);
        testConfiguration(8, 16, 40, "Large Pool", csvResults);

        // Запись результатов в CSV
        try (FileWriter writer = new FileWriter("performance_results.csv")) {
            for (String line : csvResults) {
                writer.write(line + "\n");
            }
            System.out.println("Результаты сохранены в performance_results.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testConfiguration(int corePoolSize, int maxPoolSize, int queueSize, String configName, List<String> csvResults) {
        System.out.println("\nTesting configuration: " + configName +
                " (core=" + corePoolSize + ", max=" + maxPoolSize + ", queue=" + queueSize + ")");

        CustomThreadPool customPool = new CustomThreadPool(
                corePoolSize, maxPoolSize, 1, TimeUnit.SECONDS,
                queueSize, 2, new RejectPolicy(), "CustomPool-" + configName);

        ExecutorService customPoolAdapter = new CustomThreadPoolAdapter(customPool);

        ExecutorService standardPool = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, 1, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize));

        // Тестируем каждый пул и собираем результаты
        TestResult customResult = runTest("CustomThreadPool", customPoolAdapter, true);
        TestResult standardResult = runTest("ThreadPoolExecutor", standardPool, true);

        // Обработка результатов и добавление их в CSV
        addResultToCsv(csvResults, configName, "CustomThreadPool", customResult);
        addResultToCsv(csvResults, configName, "ThreadPoolExecutor", standardResult);

        // Завершаем пулы
        customPool.shutdown();
        standardPool.shutdown();
    }

    private static void addResultToCsv(List<String> csvResults, String configName, String poolType, TestResult result) {
        // Можно расширить, чтобы считать среднее время или другие метрики
        String line = String.format("%s,%s,%d,%d,%d,%d,%d,%.2f",
                configName,
                poolType,
                result.duration,
                result.minTime,
                result.maxTime,
                result.completed,
                TASK_COUNT,
                (result.completed * 100.0) / TASK_COUNT);
        csvResults.add(line);
    }

    private static TestResult runTest(String poolType, ExecutorService pool, boolean printStats) {
        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        long startTime = System.currentTimeMillis();

        AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxTime = new AtomicLong(Long.MIN_VALUE);

        for (int i = 0; i < TASK_COUNT; i++) {
            try {
                pool.execute(() -> {
                    long taskStartInner = System.currentTimeMillis();
                    try {
                        Thread.sleep(TASK_DURATION_MS);
                        completed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                        long taskEnd = System.currentTimeMillis();
                        long taskDuration = taskEnd - taskStartInner;
                        synchronized (ComparePerformance.class) {
                            minTime.set(Math.min(minTime.get(), taskDuration));
                            maxTime.set(Math.max(maxTime.get(), taskDuration));
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                rejectedTasks.incrementAndGet();
                latch.countDown();
                if (printStats) {
                    System.out.println("Task rejected from " + pool.getClass().getSimpleName());
                }
            }
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        if (printStats) {
            System.out.printf("%s: Completed %d/%d tasks in %dms%n",
                    pool.getClass().getSimpleName(), completed.get(), TASK_COUNT, duration);
        }

        return new TestResult(duration, completed.get(), minTime.get(), maxTime.get());
    }

    private static class TestResult {
        final long duration;
        final int completed;
        final long minTime;
        final long maxTime;

        TestResult(long duration, int completed, long minTime, long maxTime) {
            this.duration = duration;
            this.completed = completed;
            this.minTime = minTime;
            this.maxTime = maxTime;
        }
    }
}