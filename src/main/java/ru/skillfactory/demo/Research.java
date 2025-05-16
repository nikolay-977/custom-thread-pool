package ru.skillfactory.demo;

import ru.skillfactory.custom.thread.pool.CustomThreadPool;
import ru.skillfactory.custom.thread.pool.CustomThreadPoolAdapter;
import ru.skillfactory.custom.thread.pool.RejectPolicy;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Research {
    private static final int TASK_COUNT = 100;
    private static final int TASK_DURATION_MS = 10;
    private static final String CSV_FILE = "research_results.csv";

    public static void main(String[] args) {
        List<String> csvResults = new ArrayList<>();
        // Записываем заголовок CSV
        csvResults.add("corePoolSize,maxPoolSize,queueSize,minSpareThreads,PoolType,AvgTimeMs,MinTimeMs,MaxTimeMs,TasksCompleted,TotalTasks,SuccessRate%");

        // Варьируем параметры пула
        for (int corePoolSize = 2; corePoolSize <= 4; corePoolSize += 2) {
            for (int maxPoolSize = 4; maxPoolSize <= 16; maxPoolSize += 2) {
                for (int queueSize = 10; queueSize <= 40; queueSize += 10) {
                    for (int minSpareThreads = 0; minSpareThreads <= 2; minSpareThreads++) {
                        System.out.printf("Testing configuration: corePoolSize=%d, maxPoolSize=%d, queueSize=%d, minSpareThreads=%d%n",
                                corePoolSize, maxPoolSize, queueSize, minSpareThreads);

                        // Проверяем параметры перед созданием пула
                        if (maxPoolSize < corePoolSize || queueSize < 1 || minSpareThreads > corePoolSize) {
                            System.out.println("Invalid thread pool parameters, skipping this configuration.");
                            continue;
                        }

                        // Создаем кастомный пул
                        CustomThreadPool customPool = new CustomThreadPool(
                                corePoolSize, maxPoolSize, 5, TimeUnit.SECONDS,
                                queueSize, minSpareThreads, new RejectPolicy(), "CustomPool");

                        ExecutorService customPoolAdapter = new CustomThreadPoolAdapter(customPool);

                        // Тестируем кастомный пул
                        TestResult customResult = runTest(customPoolAdapter);

                        // Добавляем результаты в CSV
                        addResultToCsv(csvResults, corePoolSize, maxPoolSize, queueSize, minSpareThreads, customResult);

                        // Завершаем пулы
                        customPool.shutdown();
                    }
                }
            }
        }

        // Записываем результаты в файл CSV
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_FILE))) {
            for (String line : csvResults) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Research complete. Results written to " + CSV_FILE);
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }

    private static TestResult runTest(ExecutorService pool) {
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
                        long taskEnd = System.currentTimeMillis();
                        long taskDuration = taskEnd - taskStartInner;
                        synchronized (Research.class) {
                            if (taskDuration < minTime.get()) {
                                minTime.set(taskDuration);
                            }
                            if (taskDuration > maxTime.get()) {
                                maxTime.set(taskDuration);
                            }
                        }
                        latch.countDown();
                    }
                });
            } catch (RejectedExecutionException e) {
                // Задача отклонена, уменьшаем счетчик лачт, но не увеличиваем completed
                latch.countDown();
            }
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        if (completed.get() == 0) completed.set(1); // избегать деления на 0
        long avgTime = duration / completed.get();

        System.out.printf("%s: Completed %d/%d tasks in %dms (min %dms, max %dms)%n",
                pool.getClass().getSimpleName(), completed.get(), TASK_COUNT, duration, minTime.get(), maxTime.get());

        return new TestResult(duration, completed.get(), minTime.get(), maxTime.get());
    }

    private static void addResultToCsv(List<String> csvResults,
                                       int corePoolSize, int maxPoolSize, int queueSize, int minSpareThreads, TestResult result) {
        long avgTime = result.duration / result.completed;
        double successRate = (result.completed * 100.0) / TASK_COUNT;
        String line = String.format("%d,%d,%d,%s,%d,%d,%d,%d,%d,%.2f",
                corePoolSize, maxPoolSize, queueSize, minSpareThreads,
                avgTime,
                result.minTime,
                result.maxTime,
                result.completed,
                TASK_COUNT,
                successRate);
        csvResults.add(line);
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

