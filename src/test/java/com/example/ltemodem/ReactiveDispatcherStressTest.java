package com.example.ltemodem;

import com.example.ltemodem.core.LteModemManager;
import com.example.ltemodem.events.RawResponseEvent;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

// no extra IO needed for this test
import java.time.Duration;

/**
 * Quick stress test to publish many events into the manager and verify reactive Flux delivery.
 */
public class ReactiveDispatcherStressTest {

    @Test
    public void stressReactiveFluxDelivery() throws Exception {
        // Setup a manager with piped in/out streams (simple in-memory streams used here)
    LteModemManager mgr = new LteModemManager();

    // emit many events quickly
    for (int i = 0; i < 2000; i++) {
        mgr.testEmitEvent(new RawResponseEvent("stress-" + i));
    }

    // subscribe to the Flux and consume a slice
    Flux<String> payloads = mgr.eventFlux()
        .filter(e -> e instanceof RawResponseEvent)
        .map(e -> ((RawResponseEvent) e).getRaw())
        .map(String::valueOf)
        .take(1000);

    StepVerifier.create(payloads)
        .expectNextCount(1000)
        .thenCancel()
        .verify(Duration.ofSeconds(5));
    }
}
