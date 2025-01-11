package com.dc.tools.timer;


/**
 * running tasks
 *
 * @author zy
 */
public interface Task extends Runnable {


    /**
     * task name
     */
    String taskName();

    /**
     * cancel task for next execution
     */
    void cancel();
}
