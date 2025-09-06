package com.example.ltemodem.pdp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public final class TestHelpers {
    private TestHelpers() {}

    // Call the private withRetry via reflection so production method stays private
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> callWithRetry(PdpContextManager mgr, Callable<T> task, int maxRetries, long delayMs) {
        try {
            Method m = PdpContextManager.class.getDeclaredMethod("withRetry", Callable.class, int.class, long.class);
            m.setAccessible(true);
            return (CompletableFuture<T>) m.invoke(mgr, task, maxRetries, delayMs);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
