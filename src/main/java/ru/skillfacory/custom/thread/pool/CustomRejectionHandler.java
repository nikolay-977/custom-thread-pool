package ru.skillfacory.custom.thread.pool;

import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CustomRejectionHandler implements RejectedExecutionHandler {
    private static final Logger logger = Logger.getLogger(CustomRejectionHandler.class.getName());

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (r instanceof FutureTask) {
            ((FutureTask<?>) r).cancel(false);
        }

        logger.log(Level.WARNING, "Task rejected: " + r.toString());
    }
}