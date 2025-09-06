package com.example.ltemodem.demo;

import com.example.ltemodem.events.PdpEvent;
import com.example.ltemodem.events.ModemEvent;
import com.example.ltemodem.events.ModemEventListener;
import com.example.ltemodem.events.RawResponseEvent;
import com.example.ltemodem.events.SmsReceivedEvent;
import com.example.ltemodem.events.UssdEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingListener implements ModemEventListener{
    private static final Logger LOG = LoggerFactory.getLogger(LoggingListener.class);

    @Override
    public void onEvent(final ModemEvent event) {
        if (event instanceof UssdEvent ussd) {
            LOG.info("[USSD] {}", ussd);
        } else if (event instanceof SmsReceivedEvent sms) {
            LOG.info("[SMS] {}", sms);
        } else if (event instanceof PdpEvent pdp) {
            LOG.info("[PDP] {}", pdp);
        } else if (event instanceof RawResponseEvent raw) {
            LOG.info("[RAW] {}", raw.getRaw());
        }
    }
}
