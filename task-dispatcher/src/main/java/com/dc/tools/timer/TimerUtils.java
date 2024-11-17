package com.dc.tools.timer;

import java.util.concurrent.ExecutorService;

public class TimerUtils {

    private static final Timer.Factory DEFAULT_FACTORY = new TimerFactory();

    public static Timer createTimer(String timerName, ExecutorService executorService) {
        return DEFAULT_FACTORY.createTimer(timerName);
    }

    public static Timer createTimer(String timerName) {
        return DEFAULT_FACTORY.createTimer(timerName);
    }

    static class TimerFactory implements Timer.Factory {

        @Override
        public Timer createTimer(String timerName) {
            return new DelayedTimer(timerName);
        }

        @Override
        public Timer createTimer(String timerName, ExecutorService executorService) {
            return new DelayedTimer(timerName, executorService);
        }
    }

}
