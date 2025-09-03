package com.example.ltemodem;

import java.util.regex.*;
import com.example.ltemodem.events.*;

public class LteModemApi {
    private final LteModemManager modem;
    public LteModemApi(LteModemManager modem){this.modem=modem;}

    public String getModemInfo() throws Exception { return modem.sendAndWait("ATI",3000); }
    public boolean sendSms(String number,String text) throws Exception{
        modem.sendAndWait("AT+CMGF=1",3000);
        modem.sendAndWait("AT+CMGS=\""+number+"\"",3000);
        modem.sendCommand(text+"\u001A");
        return modem.sendAndWait("",5000).contains("OK");
    }
    public boolean enableUssdNotifications() throws Exception { return modem.sendAndWait("AT+CUSD=1",3000).contains("OK"); }
    public String sendUssd(String code,int dcs) throws Exception{
        String resp=modem.sendAndWait("AT+CUSD=1,\""+code+"\","+dcs,10000);
        Pattern p=Pattern.compile("\\+CUSD:\\s*\\d+,\"([^\"]+)\",(\\d+)");
        Matcher m=p.matcher(resp);
        return m.find()?m.group(1):resp;
    }

    // ------------------ PDP / Data Session ------------------
    public boolean attachPdpContext(String apn) throws Exception{
        if(!modem.sendAndWait("AT+CGDCONT=1,\"IP\",\""+apn+"\"",3000).contains("OK")) return false;
        return modem.sendAndWait("AT+CGATT=1",5000).contains("OK");
    }
    public boolean detachPdpContext() throws Exception{ return modem.sendAndWait("AT+CGATT=0",5000).contains("OK"); }
    public boolean activatePdpContext() throws Exception{ return modem.sendAndWait("AT+CGACT=1,1",5000).contains("OK"); }
    public boolean deactivatePdpContext() throws Exception{ return modem.sendAndWait("AT+CGACT=0,1",5000).contains("OK"); }
    public String getIpAddress() throws Exception{
        String resp=modem.sendAndWait("AT+CGPADDR=1",3000);
        Pattern p=Pattern.compile("\\+CGPADDR:\\s*1,(\\S+)");
        Matcher m=p.matcher(resp);
        return m.find()?m.group(1):null;
    }
    public boolean isAttached() throws Exception{ return modem.sendAndWait("AT+CGATT?",3000).contains("1"); }
}
