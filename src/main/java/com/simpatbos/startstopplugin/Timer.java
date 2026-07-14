package com.simpatbos.startstopplugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;

import com.simpatbos.startstopplugin.AWSManager.EC2ServerStatus;

public class Timer {
    private final int timeoutInSeconds;
    private final Callable<EC2ServerStatus> task;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> shutdownTask = null;

    public Timer(int timeoutInSeconds, Callable<EC2ServerStatus> task) {
        this.timeoutInSeconds = timeoutInSeconds;
        this.task = task;
    }

    public synchronized void startTimer() {
        if (isTiming()) return;
        shutdownTask = scheduler.schedule(this.task, this.timeoutInSeconds, TimeUnit.SECONDS);
    }

    public synchronized void cancelTimer() {
        if (shutdownTask != null && !shutdownTask.isDone()) {
            shutdownTask.cancel(false);
        }
    }

    public synchronized boolean isTiming() {
        return this.shutdownTask != null 
                && !this.shutdownTask.isCancelled() 
                && !this.shutdownTask.isDone();
    }

    public synchronized long getTimeLeft() {
        if (!isTiming()) return 0;
        return this.shutdownTask.getDelay(TimeUnit.SECONDS);
    }
}
