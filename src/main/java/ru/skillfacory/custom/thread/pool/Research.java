package ru.skillfacory.custom.thread.pool;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Research {
    private static final String CSV_HEADER = "Test Name,Total,Successful,Rejected\n";

    public static void main(String[] args) {
        TimeUnit timeUnit = TimeUnit.SECONDS;

        String csvFileName = "research.csv";

        try (FileWriter writer = new FileWriter(csvFileName)) {
            writer.write(CSV_HEADER);
            // 1. Тестирование базовой функциональности
            TestResult basicTestResult = test("=== Basic Functionality Test ===", 10, 1000, 4000,2, 4, 5, timeUnit, 6, 1);
            writer.write(String.format("%s,%d,%d,%d\n",
                    "Basic Functionality",
                    basicTestResult.totalTasks,
                    basicTestResult.successfulSubmissions.get(),
                    basicTestResult.rejectedTasks.get()));
            // 2. Тестирование перегрузки
            TestResult overloadTestResult = test("=== Overload Test ===",20, 2000,7000,4, 8, 5, timeUnit, 12, 2);
            writer.write(String.format("%s,%d,%d,%d\n",
                    "Overload Test",
                    overloadTestResult.totalTasks,
                    overloadTestResult.successfulSubmissions.get(),
                    overloadTestResult.rejectedTasks.get()));
            // 3. Тестирование пиковой нагрузки
            TestResult peakLoadTestResult = test("=== Peak Load Test ===", 100, 1500, 7000,16, 32, 5, timeUnit, 70, 8);
            writer.write(String.format("%s,%d,%d,%d\n",
                    "Peak Load Test",
                    peakLoadTestResult.totalTasks,
                    peakLoadTestResult.successfulSubmissions.get(),
                    peakLoadTestResult.rejectedTasks.get()));
            System.out.println("Результаты тестов сохранены в: " + csvFileName);
        } catch (IOException e) {
            System.err.println("Ошибка при записи в CSV файл: " + e.getMessage());
        }
    }

    private static TestResult test(String name, int totalTasks, long timeTaskMillis, long waitMillis, int corePoolSize, int maxPoolSize, long keepAliveTime,
                                           TimeUnit timeUnit, int queueSize, int minSpareThreads) {
        System.out.println("\n" + name);
        TestResult result = new TestResult();
        result.totalTasks = totalTasks;
        result.rejectedTasks = new AtomicInteger(0);
        CustomThreadPool pool = new CustomThreadPool(corePoolSize, maxPoolSize, keepAliveTime, timeUnit, queueSize, minSpareThreads, new CustomThreadFactory(), new CountingRejectedExecutionHandler(result.rejectedTasks));
        for (int i = 1; i <= result.totalTasks; i++) {
            final int taskId = i;
            try {
                pool.execute(() -> {
                    System.out.println("Executing task " + taskId + " in " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(timeTaskMillis); // Увеличиваем время выполнения задачи
                        result.successfulSubmissions.incrementAndGet();
                        System.out.println("Task " + taskId + " submitted successfully");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (Exception e) {
                System.out.println("Task " + taskId + " rejected: " + e.getMessage());
            }
        }
        try {
            Thread.sleep(waitMillis); // Увеличиваем время ожидания для завершения всех задач
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pool.shutdown();
        return result;
    }

    private static class TestResult {
        int totalTasks;
        final AtomicInteger successfulSubmissions = new AtomicInteger(0);
        AtomicInteger rejectedTasks = new AtomicInteger(0);
    }
}