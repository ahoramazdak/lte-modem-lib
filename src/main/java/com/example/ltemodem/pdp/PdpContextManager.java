
package com.example.ltemodem.pdp;

import com.example.ltemodem.core.LteModemManager;
import com.example.ltemodem.core.LteModemApi;
import com.example.ltemodem.events.ModemEvent;
import com.example.ltemodem.events.PdpEvent;
import com.example.ltemodem.core.EventDispatcher;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Manager responsible for PDP attach/activate/deactivate flows.
 */
public final class PdpContextManager {

    /** API wrapper used to send AT commands. */
    private final LteModemApi api;

    /** Manager that provides modem IO and event dispatch. */
    private final LteModemManager manager;

    /** Scheduler used for timeouts and retry scheduling. */
    private final ScheduledExecutorService scheduler = Executors
            .newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "pdp-scheduler");
        t.setDaemon(true);
        return t;
    });
    // separate executor for blocking AT commands
    /** Executor for blocking AT commands. */
    private final ExecutorService commandExecutor = Executors
            .newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "pdp-commands");
                t.setDaemon(true);
                return t;
            });
    /** Dispatcher for delivering modem events to PDP waiters. */
    private final EventDispatcher dispatcher = new EventDispatcher();
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 500L;
    /**
     * Create a new manager.
     *
     * @param apiClient API used to send AT commands
     * @param managerClient modem manager that emits events
     */
    public PdpContextManager(final LteModemApi apiClient, final LteModemManager managerClient) {
        this.api = apiClient;
        this.manager = managerClient;
    }

    /**
     * Attempt to attach PDP context for the given APN.
     *
     * @param apn APN to attach
     * @param timeoutMs timeout in milliseconds for operation
     * @return future that completes true on success
     */
    public CompletableFuture<Boolean> attachAsync(final String apn, final long timeoutMs) {
        // Execute the attach command after the event listener is registered to avoid
        // losing simulated OK responses that may arrive before the command is sent.
        return sendAndAwaitPdp(true, () -> api.attachPdpContext(apn), timeoutMs, PdpEvent::isAttached);
    }

    /**
     * Detach PDP context.
     *
     * @param timeoutMs timeout in milliseconds
     * @return future that completes true on success
     */
    public CompletableFuture<Boolean> detachAsync(final long timeoutMs) {
        return sendAndAwaitPdp(false, api::detachPdpContext, timeoutMs, pdp -> !pdp.isAttached());
    }

    /**
     * Activate PDP context.
     *
     * @param timeoutMs timeout in milliseconds
     * @return future that completes true on success
     */
    public CompletableFuture<Boolean> activateAsync(final long timeoutMs) {
        return sendAndAwaitPdp(true, api::activatePdpContext, timeoutMs, pdp -> pdp.isActivated());
    }

    /**
     * Deactivate PDP context.
     *
     * @param timeoutMs timeout in milliseconds
     * @return future that completes true on success
     */
    public CompletableFuture<Boolean> deactivateAsync(final long timeoutMs) {
        return sendAndAwaitPdp(false, api::deactivatePdpContext, timeoutMs, pdp -> !pdp.isActivated());
    }
    private <T> CompletableFuture<T> withRetry(final Callable<T> task, final int maxRetries, final long delayMs) {
        CompletableFuture<T> future = new CompletableFuture<>();
        final AtomicInteger attempt = new AtomicInteger(0);

        Runnable runAttempt = new Runnable() {
            @Override
            public void run() {
                if (future.isDone()) {
                    return;
                }

                commandExecutor.submit(() -> {
                    try {
                        final T res = task.call();
                        future.complete(res);
                    } catch (final Exception e) {
                        final int i = attempt.getAndIncrement();
                        if (i >= maxRetries - 1) {
                            future.completeExceptionally(e);
                        } else {
                            final long delay = delayMs * (1L << i);
                            scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
                        }
                    }
                });
            }
        };

        // start first attempt
        scheduler.execute(runAttempt);
        return future;
    }

    private CompletableFuture<Boolean> sendAndAwaitPdp(
        final boolean targetState,
        final Callable<Boolean> command,
        final long timeoutMs,
        final java.util.function.Predicate<PdpEvent> condition
    ) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        final Consumer<ModemEvent> listener = evt -> {
            if (evt instanceof PdpEvent pdp && condition.test(pdp)) {
                future.complete(true);
            }
        };

        // register consumer directly so we can remove the same instance
        manager.addListener(listener);

        ScheduledFuture<?> timeout = scheduler.schedule(() -> {
            future.completeExceptionally(new TimeoutException("PDP operation timed out"));
        }, timeoutMs, TimeUnit.MILLISECONDS);

        // Execute the command with retries AFTER listener registration so we don't
        // miss incoming events; propagate any failures to the returned future.
    withRetry(command, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_DELAY_MS).whenComplete((ok, ex) -> {
            if (ex != null) {
                future.completeExceptionally(ex);
            } else if (!ok) {
                future.completeExceptionally(new IOException("AT command failed"));
            }
        });

        future.whenComplete((res, ex) -> {
            timeout.cancel(true);
            manager.removeListener(listener);
        });

        return future;
    }

    /**
     * Shut down internal executors.
     */
    public void shutdown() {
        scheduler.shutdownNow();
        commandExecutor.shutdownNow();
    }
}

