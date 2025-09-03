package com.example.ltemodem.demo;
import com.example.ltemodem.events.*;
public class LoggingListener implements ModemEventListener{
    @Override public void onEvent(ModemEvent event){
        if(event instanceof UssdEvent ussd) System.out.println("[USSD] "+ussd);
        else if(event instanceof SmsReceivedEvent sms) System.out.println("[SMS] "+sms);
        else if(event instanceof PdpEvent pdp) System.out.println("[PDP] "+pdp);
        else if(event instanceof RawResponseEvent raw) System.out.println("[RAW] "+raw.getRaw());
    }
}
