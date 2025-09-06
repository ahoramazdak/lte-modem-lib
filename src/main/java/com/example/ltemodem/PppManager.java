package com.example.ltemodem;

import com.example.ltemodem.core.LteModemApi;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PppManager {
    /** API used to manage PDP attach/activate operations. */
    private final LteModemApi api;

    /** Background PPP process instance when running. */
    private Process pppProcess;

    private static final Logger LOG = LoggerFactory.getLogger(PppManager.class);

    private static final String PPP_DEVICE = "ttyUSB2";

    private static final int PPP_BAUD = 115200;

    private static final long PPP_START_WAIT_MS = 5000L;
    public PppManager(final LteModemApi apiClient) {
        this.api = apiClient;
    }

    public boolean startPPP(final String apn) throws Exception {
        if (!api.attachPdpContext(apn)) {
            return false;
        }

        ProcessBuilder pb = new ProcessBuilder(
                "pppd",
                PPP_DEVICE,
                Integer.toString(PPP_BAUD),
                "defaultroute",
                "usepeerdns",
                "noauth",
                "nodetach"
        );
        pb.redirectErrorStream(true);
    pppProcess = pb.start();
    Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ppp-reader");
            t.setDaemon(true);
            return t;
        }).submit(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(pppProcess.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    LOG.info("[PPP] {}", line);
                }
            } catch (IOException e) {
                LOG.error("PPP process reader failed", e);
            }
        });
    Thread.sleep(PPP_START_WAIT_MS);
        return pppProcess.isAlive();
    }

    public void stopPPP() {
        if (pppProcess != null) {
            pppProcess.destroy();
        }
    }
}
