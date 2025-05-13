package ru.skillfacory.custom.thread.pool;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Инициализация пула с параметрами
        CustomThreadPool pool = new CustomThreadPool(
                2, // corePoolSize
                4, // maxPoolSize
                5, // keepAliveTime
                TimeUnit.SECONDS, // timeUnit
                5, // queueSize
                1  // minSpareThreads
        );

        // Отправляем задачи в пул
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            try {
                pool.execute(() -> {
                    System.out.println("[Task] " + taskId + " started by " + Thread.currentThread().getName());
                    try {
                        // Имитация работы
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("[Task] " + taskId + " completed by " + Thread.currentThread().getName());
                });
            } catch (Exception e) {
                System.out.println("[Main] Error submitting task " + taskId + ": " + e.getMessage());
            }
            Thread.sleep(300); // Задержка между отправкой задач
        }

        // Даем время на выполнение задач
        Thread.sleep(5000);

        // Завершаем пул
        pool.shutdown();

        // Пробуем отправить задачу после shutdown
        try {
            pool.execute(() -> System.out.println("This task should be rejected"));
        } catch (Exception e) {
            System.out.println("[Main] Expected rejection after shutdown: " + e.getMessage());
        }

        // Даем время на завершение всех задач
        Thread.sleep(3000);
        System.out.println("[Main] All tasks completed");
    }
}