package com.dc.tools.common.window;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * LeapWindow 简单使用方式
 */
public class LeapWindowExample {

    public static void main(String[] args) {

        //1、创建LeapWindow
        LeapWindow<HealthState, HealthState, HealthState> leapWindow =
                new LeapWindow<>(10, Duration.ofSeconds(1), HealthState::new, HealthState::addState, HealthState::new, HealthState::addState);

        /**
         * 可以多次启动
         */
        LeapWindow.Publish<HealthState> publish = leapWindow.start();

        AtomicLong atomicLong = new AtomicLong();
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {

            System.out.println("dispose leapWindow");
            leapWindow.dispose();

        }, 10, TimeUnit.SECONDS);

        Executors.newScheduledThreadPool(2).scheduleAtFixedRate(() -> {

            long value = atomicLong.incrementAndGet();
            HealthState healthState = new HealthState();

            if (value % 2 == 0) {
                healthState.addExcept();
            }

            healthState.addPass();
            publish.publish(healthState);
        }, 0, 50, TimeUnit.MILLISECONDS);


        Executors.newScheduledThreadPool(2).scheduleAtFixedRate(() -> {

            long value = atomicLong.incrementAndGet();
            HealthState healthState = new HealthState();

            if (value % 2 == 0) {
                healthState.addExcept();
            }

            healthState.addPass();
            publish.publish(healthState);
        }, 0, 10, TimeUnit.MILLISECONDS);


    }

    static class HealthState {

        LongAdder[] state;

        HealthState() {
            state = new LongAdder[2];
            state[0] = new LongAdder();
            state[1] = new LongAdder();
        }

        public void addPass() {
            state[0].increment();
        }

        public void addExcept() {
            state[1].increment();
        }

        public long getExcept() {
            return state[1].sum();
        }


        public HealthState addState(HealthState healthState) {
            this.state[0].add(healthState.state[0].sum());
            this.state[1].add(healthState.state[1].sum());
            return this;
        }

        @Override
        public String toString() {
            return "HealthState{" +
                    "pass=" + state[0].sum() + "," +
                    "except=" + state[1].sum() + "," +
                    '}';
        }
    }
}
