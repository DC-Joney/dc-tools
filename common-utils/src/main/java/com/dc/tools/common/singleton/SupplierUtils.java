package com.dc.tools.common.singleton;


import com.dc.tools.common.annotaion.Nullable;

import java.util.function.Supplier;

/**
 * Convenience utilities for {@link Supplier} handling.
 *
 * @apiNote Fork from spring
 */
public abstract class SupplierUtils {

    /**
     * Resolve the given {@code Supplier}, getting its result or immediately
     * returning {@code null} if the supplier itself was {@code null}.
     *
     * @param supplier the supplier to resolve
     * @return the supplier's result, or {@code null} if none
     */
    @Nullable
    public static <T> T resolve(@Nullable Supplier<T> supplier) {
        return (supplier != null ? supplier.get() : null);
    }

}
