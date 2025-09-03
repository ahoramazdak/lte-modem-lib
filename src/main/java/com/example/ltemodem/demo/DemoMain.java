package com.example.ltemodem.demo;
import com.example.ltemodem.*;
public class DemoMain {
    public static void main(String[] args) throws Exception{
        LteModemManager modem=new LteModemManager();
        modem.addListener(new LoggingListener());
        if(!modem.connect("/dev/ttyUSB2",115200)) return;
        LteModemApi api=new LteModemApi(modem);
        PppManager ppp=new PppManager(api);

        System.out.println("Modem Info:\n"+api.getModemInfo());
        api.enableUssdNotifications();
        api.sendUssd("*123#",15);
        api.sendSms("+491701234567","Hello from Java LTE!");

        if(ppp.startPPP("internet")) System.out.println("PPP session started! IP routing active.");

        Thread.sleep(15000);
        ppp.stopPPP();
        modem.disconnect();
modem.connect("/dev/ttyUSB2", 115200);
LteModemApi api = new LteModemApi(modem);
PdpContextManager pdpMgr = new PdpContextManager(api, modem);

pdpMgr.attachAsync("internet", 5000)
      .thenAccept(ok -> System.out.println("Attached: " + ok))
      .exceptionally(ex -> {
          System.err.println("Attach failed: " + ex.getMessage());
          return null;
      });

Thread.sleep(6000);
pdpMgr.shutdown();
modem.disconnect();
    }
}
