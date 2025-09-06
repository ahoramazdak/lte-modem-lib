package com.example.ltemodem;

import com.example.ltemodem.core.LteModemManager;
import com.example.ltemodem.core.LteModemApi;
import com.example.ltemodem.events.*;
import com.example.ltemodem.mock.MockSerialPort;
import com.example.ltemodem.pdp.PdpContextManager;
import org.junit.jupiter.api.*;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class PdpContextManagerTest {

    private MockSerialPort mockPort;
    private LteModemManager modem;
    private LteModemApi api;
    private PdpContextManager pdpMgr;

    @BeforeEach
    void setup() throws Exception {
        mockPort = new MockSerialPort();
        modem = new LteModemManager(mockPort.getInputStream(), mockPort.getOutputStream());
        api = new LteModemApi(modem);
        pdpMgr = new PdpContextManager(api, modem);
    }

    @AfterEach
    void tearDown() {
        pdpMgr.shutdown();
    }

    @Test
    void testAttachAsyncCompletesOnEvent() throws Exception {
        CompletableFuture<Boolean> fut = pdpMgr.attachAsync("internet", 1000);
        mockPort.simulateIncomingLine("OK");
        mockPort.simulateIncomingLine("+CGATT: 1");
        assertTrue(fut.get(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void testDetachAsyncCompletesOnEvent() throws Exception {
        CompletableFuture<Boolean> fut = pdpMgr.detachAsync(1000);
        mockPort.simulateIncomingLine("OK");
        mockPort.simulateIncomingLine("+CGATT: 0");
        assertTrue(fut.get(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void testActivateAsyncCompletesOnEvent() throws Exception {
        CompletableFuture<Boolean> fut = pdpMgr.activateAsync(1000);
        mockPort.simulateIncomingLine("OK");
        mockPort.simulateIncomingLine("+CGEV: PDN ACT");
        assertTrue(fut.get(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void testDeactivateAsyncCompletesOnEvent() throws Exception {
        CompletableFuture<Boolean> fut = pdpMgr.deactivateAsync(1000);
        mockPort.simulateIncomingLine("OK");
        mockPort.simulateIncomingLine("+CGEV: PDN DEACT");
        assertTrue(fut.get(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void testAttachAsyncTimeout() {
        CompletableFuture<Boolean> fut = pdpMgr.attachAsync("internet", 200);
        assertThrows(ExecutionException.class, () -> fut.get(500, TimeUnit.MILLISECONDS));
    }
}

