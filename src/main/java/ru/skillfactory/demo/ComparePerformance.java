package ru.skillfactory.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(ComparePerformance.class);
    private static final int TASK_COUNT = 100;
    private static final int TASK_DURATION_MS = 10;
    private static final AtomicInteger rejectedTasks = new AtomicInteger(0);

    public static void main(String[] args) {
        logger.info("Performance Comparison between CustomThreadPool and Standard Pools");
        logger.info("================================================================");

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
            logger.info("Результаты сохранены в performance_results.csv");
        } catch (IOException e) {
            logger.error("Ошибка при записи результатов в файл", e);
        }
    }

    private static void testConfiguration(int corePoolSize, int maxPoolSize, int queueSize,
                                          String configName, List<String> csvResults) {
        logger.info("\nTesting configuration: {} (core={}, max={}, queue={})",
                configName, corePoolSize, maxPoolSize, queueSize);

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
        shutdownPool(customPoolAdapter, "CustomThreadPool");
        shutdownPool(standardPool, "ThreadPoolExecutor");
    }

    private static void shutdownPool(ExecutorService pool, String poolType) {
        logger.debug("Завершение работы пула {}", poolType);
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Принудительное завершение пула {}", poolType);
                List<Runnable> remainingTasks = pool.shutdownNow();
                logger.warn("{} задач не были выполнены в пуле {}", remainingTasks.size(), poolType);
            }
        } catch (InterruptedException e) {
            logger.error("Прерывание при завершении пула {}", poolType, e);
            Thread.currentThread().interrupt();
        }
    }

    private static void addResultToCsv(List<String> csvResults, String configName,
                                       String poolType, TestResult result) {
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

    private static TestResult runTest(String poolType, ExecutorService pool, boolean logDetails) {
        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        long startTime = System.currentTimeMillis();
        AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxTime = new AtomicLong(Long.MIN_VALUE);

        logger.debug("Начало тестирования пула {}", poolType);

        for (int i = 0; i < TASK_COUNT; i++) {
            final int taskNum = i;
            try {
                pool.execute(() -> {
                    long taskStart = System.currentTimeMillis();
                    try {
                        Thread.sleep(TASK_DURATION_MS);
                        completed.incrementAndGet();
                        logger.trace("Задача {} выполнена в пуле {}", taskNum, poolType);
                    } catch (InterruptedException e) {
                        logger.warn("Задача {} прервана", taskNum, e);
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                        long taskDuration = System.currentTimeMillis() - taskStart;
                        updateMinMax(taskDuration, minTime, maxTime);
                    }
                });
            } catch (RejectedExecutionException e) {
                rejectedTasks.incrementAndGet();
                latch.countDown();
                logger.warn("Задача {} отклонена пулом {}", taskNum, poolType);
            }
        }

        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                logger.warn("Таймаут ожидания завершения задач в пуле {}", poolType);
            }
        } catch (InterruptedException e) {
            logger.error("Прерывание при ожидании завершения задач", e);
            Thread.currentThread().interrupt();
        }

        long duration = System.currentTimeMillis() - startTime;

        if (logDetails) {
            logger.info("{}: выполнено {}/{} задач за {} мс (мин: {} мс, макс: {} мс)",
                    poolType, completed.get(), TASK_COUNT, duration, minTime.get(), maxTime.get());
        }

        return new TestResult(duration, completed.get(), minTime.get(), maxTime.get());
    }

    private static synchronized void updateMinMax(long duration, AtomicLong minTime, AtomicLong maxTime) {
        minTime.set(Math.min(minTime.get(), duration));
        maxTime.set(Math.max(maxTime.get(), duration));
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