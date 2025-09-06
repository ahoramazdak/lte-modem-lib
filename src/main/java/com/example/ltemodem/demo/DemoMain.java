package com.example.ltemodem.demo;

import com.example.ltemodem.core.LteModemManager;
import com.example.ltemodem.core.LteModemApi;
import com.example.ltemodem.PppManager;
import com.example.ltemodem.pdp.PdpContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoMain {
    private static final Logger LOG = LoggerFactory.getLogger(DemoMain.class);

    private DemoMain() {
        // prevent instantiation
    }

    public static void main(final String[] args) throws Exception {
        LteModemManager modem = new LteModemManager();
        modem.addListener(event -> LOG.info("Received Modem Event: {}", event));
        if (!modem.connect("/dev/ttyUSB2", 115200)) {
            return;
        }
    LteModemApi api = new LteModemApi(modem);
    PppManager ppp = new PppManager(api);

    LOG.info("Modem Info:\n{}", api.getModemInfo());
        api.enableUssdNotifications();
        api.sendUssd("*123#",15);
        api.sendSms("+491701234567","Hello from Java LTE!");

        if (ppp.startPPP("internet")) {
            LOG.info("PPP session started! IP routing active.");
        }

        Thread.sleep(15000);
        ppp.stopPPP();
        modem.disconnect();
        modem.connect("/dev/ttyUSB2", 115200);
        PdpContextManager pdpMgr = new PdpContextManager(api, modem);

        pdpMgr.attachAsync("internet", 5000)
              .thenAccept(ok -> LOG.info("Attached: {}", ok))
              .exceptionally(ex -> {
                  LOG.error("Attach failed: {}", ex.getMessage());
                  return null;
              });

        Thread.sleep(6000);
        pdpMgr.shutdown();
        modem.disconnect();
    }
}
