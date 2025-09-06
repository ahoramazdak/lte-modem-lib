package com.example.ltemodem;
import com.example.ltemodem.core.LteModemManager;
import com.example.ltemodem.events.*;
import com.example.ltemodem.mock.MockSerialPort;
import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
public class LteModemManagerTest {
    private MockSerialPort mockPort;
    private LteModemManager modem;
    @BeforeEach public void setup() throws Exception { mockPort=new MockSerialPort(); modem=new LteModemManager(mockPort.getInputStream(),mockPort.getOutputStream()); }

    @Test public void testUssdEventParsing() throws Exception {
        AtomicBoolean eventReceived=new AtomicBoolean(false);
        modem.addListener(event->{ if(event instanceof UssdEvent ussd){ assertEquals("Your balance is 5.20 EUR",ussd.getMessage()); eventReceived.set(true); }});
        mockPort.simulateIncomingLine("+CUSD: 0,\"Your balance is 5.20 EUR\",15");
        Thread.sleep(200); assertTrue(eventReceived.get());
    }

    @Test public void testSmsReceivedEventParsing() throws Exception {
        AtomicBoolean eventReceived=new AtomicBoolean(false);
        modem.addListener(event->{ if(event instanceof SmsReceivedEvent sms){ assertEquals(3,sms.getIndex()); assertEquals("SM",sms.getStorage()); eventReceived.set(true); }});
        mockPort.simulateIncomingLine("+CMTI: \"SM\",3");
        Thread.sleep(200); assertTrue(eventReceived.get());
    }

    @Test public void testPdpEventParsing() throws Exception {
        AtomicBoolean eventReceived=new AtomicBoolean(false);
        modem.addListener(event->{ if(event instanceof PdpEvent pdp){ assertTrue(pdp.isAttached()); eventReceived.set(true); }});
        mockPort.simulateIncomingLine("+CGATT: 1");
        Thread.sleep(200); assertTrue(eventReceived.get());
    }
}
