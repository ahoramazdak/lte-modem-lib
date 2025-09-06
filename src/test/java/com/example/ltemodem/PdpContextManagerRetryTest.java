package com.example.ltemodem;

import com.example.ltemodem.pdp.PdpContextManager;
import com.example.ltemodem.core.LteModemApi;
import com.example.ltemodem.core.LteModemManager;
import com.example.ltemodem.mock.MockSerialPort;
import com.example.ltemodem.pdp.TestHelpers;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class PdpContextManagerRetryTest {

    @Test
    void testWithRetrySucceedsAfterFailures() throws Exception {
        MockSerialPort mockPort = new MockSerialPort();
        LteModemManager modem = new LteModemManager(mockPort.getInputStream(), mockPort.getOutputStream());
        LteModemApi api = new LteModemApi(modem);
        PdpContextManager mgr = new PdpContextManager(api, modem);

        AtomicInteger attempts = new AtomicInteger(0);
        Callable<Boolean> flaky = () -> {
            int a = attempts.incrementAndGet();
            if (a < 3) throw new IOException("transient");
            return true;
        };

    CompletableFuture<Boolean> fut = TestHelpers.callWithRetry(mgr, flaky, 5, 50);
        assertTrue(fut.get(2, TimeUnit.SECONDS));
        assertEquals(3, attempts.get());

        mgr.shutdown();
    }
}
