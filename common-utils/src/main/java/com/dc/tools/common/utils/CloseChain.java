package com.dc.tools.common.utils;

import com.google.common.io.Closer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.io.Closeable;
import java.io.IOException;

/**
 * @deprecated see {@link CloseTasks}
 *
 * @author zhangyang
 */
@SuppressWarnings("all")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Deprecated
public class CloseChain {

    @Getter
    Closer closer;

    public static CloseChain create() {
        return new CloseChain(Closer.create());
    }

    public CloseChain register(Closeable closeable) {
        this.closer.register(closeable);
        return this;
    }

    public <T extends Closeable> T addCloseable(T closeable) {
        Assert.notNull(closeable, "The closeable instance must not be null");
        this.closer.register(closeable);
        return closeable;
    }


    public void close() throws IOException {
        closer.close();
    }


}
