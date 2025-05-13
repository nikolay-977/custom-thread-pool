package ru.skillfacory.custom.thread.pool;

public class TaskWrapper implements Runnable {
    private final Runnable task;
    private final String taskName;
    private final String taskId;

    public TaskWrapper(Runnable task, String name, String taskId) {
        this.task = task;
        this.taskName = name;
        this.taskId = taskId;
    }

    @Override
    public void run() {
        task.run();
    }

    @Override
    public String toString() {
        return String.format("%s [ID: %s]", taskName, taskId);
    }
}