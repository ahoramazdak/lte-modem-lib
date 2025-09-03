
package com.example.ltemodem.pdp;

import com.example.ltemodem.*;
import com.example.ltemodem.events.*;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PdpContextManager {

    private final LteModemApi api;
    private final LteModemManager manager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public PdpContextManager(LteModemApi api, LteModemManager manager) {
        this.api = api;
        this.manager = manager;
    }

    public CompletableFuture<Boolean> attachAsync(String apn, long timeoutMs) {
        return sendAndAwaitPdp(true, () -> api.attachPdpContext(apn), timeoutMs, PdpEvent::isAttached);
    }

    public CompletableFuture<Boolean> detachAsync(long timeoutMs) {
        return sendAndAwaitPdp(false, api::detachPdpContext, timeoutMs, pdp -> !pdp.isAttached());
    }

    public CompletableFuture<Boolean> activateAsync(long timeoutMs) {
        return sendAndAwaitPdp(true, api::activatePdpContext, timeoutMs, pdp -> pdp.isActivated());
    }

    public CompletableFuture<Boolean> deactivateAsync(long timeoutMs) {
        return sendAndAwaitPdp(false, api::deactivatePdpContext, timeoutMs, pdp -> !pdp.isActivated());
    }

    private CompletableFuture<Boolean> sendAndAwaitPdp(
        boolean targetState,
        Callable<Boolean> command,
        long timeoutMs,
        java.util.function.Predicate<PdpEvent> condition
    ) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Consumer<ModemEvent> listener = evt -> {
            if (evt instanceof PdpEvent pdp && condition.test(pdp)) {
                future.complete(true);
            }
        };

        manager.addListener(listener::accept);

        ScheduledFuture<?> timeout = scheduler.schedule(() -> {
            future.completeExceptionally(new TimeoutException("PDP operation timed out"));
        }, timeoutMs, TimeUnit.MILLISECONDS);

        CompletableFuture.runAsync(() -> {
            try {
                boolean ok = command.call();
                if (!ok) {
                    future.completeExceptionally(new IOException("AT command failed"));
                }
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        future.whenComplete((res, ex) -> {
            timeout.cancel(true);
            manager.removeListener(listener::accept);
        });

        return future;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}

