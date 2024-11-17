package com.dc.tools.common.utils;

import com.google.common.io.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * This class adds back functionality that was removed in Guava v16.0.
 */
@SuppressWarnings("all")
public class CloseableUtils {
    private static final Logger log = LoggerFactory.getLogger(CloseableUtils.class);

    /**
     * <p>
     * This method has been added because Guava has removed the
     * {@code closeQuietly()} method from {@code Closeables} in v16.0. It's
     * tempting simply to replace calls to {@code closeQuietly(closeable)}
     * with calls to {@code close(closeable, true)} to close
     * {@code Closeable}s while swallowing {@code IOException}s, but
     * {@code close()} is declared as {@code throws IOException} whereas
     * {@code closeQuietly()} is not, so it's not a drop-in replacement.
     * </p>
     * <p>
     * On the whole, Guava is very backwards compatible. By fixing this nit,
     * Curator can continue to support newer versions of Guava without having
     * to bump its own dependency version.
     * </p>
     * <p>
     * See <a href="https://issues.apache.org/jira/browse/CURATOR-85">https://issues.apache.org/jira/browse/CURATOR-85</a>
     * </p>
     */
    public static void closeQuietly(Closeable closeable) {
        try {
            // Here we've instructed Guava to swallow the IOException
            Closeables.close(closeable, true);
        } catch (IOException e) {
            // We instructed Guava to swallow the IOException, so this should
            // never happen. Since it did, log it.
            log.error("IOException should not have been thrown.", e);
        }
    }
}