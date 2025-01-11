package com.dc.tools.timer;

import java.util.concurrent.Executor;

public class TimerUtils {

    private static final Timer.Factory DEFAULT_FACTORY = new TimerFactory();

    public static Timer createTimer(String timerName, Executor executor) {
        return DEFAULT_FACTORY.createTimer(timerName, executor);
    }

    public static Timer createTimer(String timerName) {
        Timer timer = DEFAULT_FACTORY.createTimer(timerName);
        timer.start();
        return timer;
    }

    static class TimerFactory implements Timer.Factory {

        @Override
        public Timer createTimer(String timerName) {
            return new DelayedTimer(timerName);
        }


        @Override
        public Timer createTimer(String timerName, Executor executorService) {
            return new DelayedTimer(timerName, executorService);
        }
    }

}
