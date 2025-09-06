package com.example.ltemodem;

import com.example.ltemodem.core.LteModemManager;
import com.example.ltemodem.events.RawResponseEvent;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration-style stress test that simulates a serial stream by writing lines
 * into a PipedOutputStream consumed by LteModemManager's reader thread.
 *
 * Configurable system properties:
 * - stress.events (int) default 5000
 * - stress.rateMs (long) delay between writes in milliseconds, default 1
 * - stress.timeoutSec (int) seconds to wait for all events, default 60
 */
public class IntegrationSerialStressTest {

    @Test
    public void runStress() throws Exception {
    boolean ci = "true".equalsIgnoreCase(System.getenv("CI"))
        || "true".equalsIgnoreCase(System.getProperty("ci", "false"));
    final int defaultEvents = ci ? 500 : 5000;
    final int events = Integer.parseInt(System.getProperty("stress.events", String.valueOf(defaultEvents)));
        final long rateMs = Long.parseLong(System.getProperty("stress.rateMs", "1"));
        final int timeoutSec = Integer.parseInt(System.getProperty("stress.timeoutSec", "60"));
    final String sinkStrategy = System.getProperty("reactor.sink.strategy", "replay");

    System.out.println("IntegrationSerialStressTest settings: events=" + events + " rateMs=" + rateMs + " timeoutSec=" + timeoutSec + " sink=" + sinkStrategy + " ci=" + ci);

        final PipedOutputStream pos = new PipedOutputStream();
        final PipedInputStream pis = new PipedInputStream(pos, 64 * 1024);

    // ensure the dispatcher picks up configured sink strategy via system property
    System.setProperty("reactor.sink.strategy", sinkStrategy);
    LteModemManager mgr = new LteModemManager(pis, new ByteArrayOutputStream());

        final CountDownLatch latch = new CountDownLatch(events);
        final AtomicInteger received = new AtomicInteger(0);
        final AtomicLong sumLatencyNs = new AtomicLong(0);
        final AtomicLong minLatencyNs = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong maxLatencyNs = new AtomicLong(0);

        // subscribe to reactive flux and measure latency by parsing the embedded timestamp
        mgr.eventFlux()
                .filter(e -> e instanceof RawResponseEvent)
                .map(e -> ((RawResponseEvent) e).getRaw())
                .subscribe(payload -> {
                    try {
                        // payload format: evt:<seq>:<nanoTs>
                        final String[] parts = payload.split(":");
                        if (parts.length >= 3) {
                            final long sentNs = Long.parseLong(parts[2]);
                            final long nowNs = System.nanoTime();
                            final long latency = nowNs - sentNs;
                            sumLatencyNs.addAndGet(latency);
                            minLatencyNs.updateAndGet(prev -> Math.min(prev, latency));
                            maxLatencyNs.updateAndGet(prev -> Math.max(prev, latency));
                        }
                    } finally {
                        received.incrementAndGet();
                        latch.countDown();
                    }
                });

        // writer thread writes lines at the configured rate
        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < events; i++) {
                    final long ts = System.nanoTime();
                    final String line = "evt:" + i + ":" + ts + "\n";
                    pos.write(line.getBytes(StandardCharsets.UTF_8));
                    pos.flush();
                    if (rateMs > 0) {
                        Thread.sleep(rateMs);
                    }
                }
            } catch (Exception ex) {
                // write errors end test prematurely
                throw new RuntimeException(ex);
            } finally {
                try {
                    pos.close();
                } catch (Exception ignored) {
                }
            }
        }, "stress-writer");

        final long start = System.nanoTime();
        writer.setDaemon(true);
        writer.start();

        final boolean ok = latch.await(timeoutSec, TimeUnit.SECONDS);
        final long end = System.nanoTime();
        final int got = received.get();

        // compute stats
        final double avgLatencyMs = got > 0 ? (sumLatencyNs.get() / (double) got) / 1_000_000.0 : 0.0;
        final double minMs = minLatencyNs.get() == Long.MAX_VALUE ? 0.0 : minLatencyNs.get() / 1_000_000.0;
        final double maxMs = maxLatencyNs.get() / 1_000_000.0;
        final double totalSec = (end - start) / 1_000_000_000.0;
        final double throughput = got / totalSec;

        System.out.println("Integration stress results: events=" + events + " got=" + got + " time(s)=" + totalSec);
        System.out.println(String.format("latency ms: avg=%.3f min=%.3f max=%.3f", avgLatencyMs, minMs, maxMs));
        System.out.println(String.format("throughput events/sec: %.1f", throughput));

        // assert all events delivered
        assertTrue(ok, "Timed out waiting for events; received=" + got + " expected=" + events);
        assertEquals(events, got, "Event count mismatch");

        // cleanup
        mgr.testEmitEvent(null); // no-op but keeps symmetric API usage
        mgr.disconnect();
    }
}
