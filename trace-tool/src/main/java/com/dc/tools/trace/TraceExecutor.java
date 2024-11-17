package com.dc.tools.trace;

import lombok.AllArgsConstructor;
import org.springframework.lang.NonNull;

import java.util.concurrent.Executor;

/**
 * 用于包装Executor 实现，保证链路的完整性
 * @author zhangyang
 */
@AllArgsConstructor(staticName = "create")
public class TraceExecutor implements Executor {

    Executor delegate;

    @Override
    public void execute(@NonNull Runnable command) {
        command = AsyncRunnable.async(command);
        delegate.execute(command);
    }
}
