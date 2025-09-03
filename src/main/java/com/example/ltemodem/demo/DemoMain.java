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
    }
}
