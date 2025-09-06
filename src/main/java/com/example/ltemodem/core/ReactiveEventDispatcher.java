package com.example.ltemodem.core;

import com.example.ltemodem.events.ModemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Small reactive dispatcher that publishes ModemEvent instances as a Flux stream.
 * This is intended for components that prefer a Reactor-based event architecture.
 */
public final class ReactiveEventDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(ReactiveEventDispatcher.class);

    // Choose sink strategy via system property 'reactor.sink.strategy':
    //  - 'replay' (default) -> Sinks.many().replay().limit(4096)
    //  - 'multicast'       -> Sinks.many().multicast().onBackpressureBuffer()
    private final Sinks.Many<ModemEvent> sink;

    public ReactiveEventDispatcher() {
        final String strategy = System.getProperty("reactor.sink.strategy", "replay");
        if ("multicast".equalsIgnoreCase(strategy)) {
            LOG.info("ReactiveEventDispatcher using multicast sink strategy");
            sink = Sinks.many().multicast().onBackpressureBuffer();
        } else {
            LOG.info("ReactiveEventDispatcher using replay sink strategy (limit=4096)");
            sink = Sinks.many().replay().limit(4096);
        }
    }

    /**
     * Return a hot Flux of ModemEvent that subscribers can consume.
     */
    public Flux<ModemEvent> flux() {
        return sink.asFlux();
    }

    /** Emit an event into the reactive stream. */
    public void emit(final ModemEvent event) {
        if (event == null) {
            // ignore null sentinel used by some tests
            return;
        }
        final Sinks.EmitResult res = sink.tryEmitNext(event);
        if (res.isFailure()) {
            LOG.warn("ReactiveEventDispatcher failed to emit event: {} -> {}", event, res);
        }
    }

    /** Complete the reactive stream. */
    public void complete() {
        sink.tryEmitComplete();
    }
}
