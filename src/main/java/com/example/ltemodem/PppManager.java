package com.example.ltemodem;

import java.io.*;
import java.util.concurrent.*;

public class PppManager {
    private final LteModemApi api;
    private Process pppProcess;
    public PppManager(LteModemApi api){this.api=api;}

    public boolean startPPP(String apn) throws Exception{
        if(!api.attachPdpContext(apn)) return false;
        ProcessBuilder pb=new ProcessBuilder("pppd","ttyUSB2","115200","defaultroute","usepeerdns","noauth","nodetach");
        pb.redirectErrorStream(true);
        pppProcess=pb.start();
        Executors.newSingleThreadExecutor().submit(()->{
            try(BufferedReader br=new BufferedReader(new InputStreamReader(pppProcess.getInputStream()))){
                String line; while((line=br.readLine())!=null) System.out.println("[PPP] "+line);
            }catch(IOException e){e.printStackTrace();}
        });
        Thread.sleep(5000);
        return pppProcess.isAlive();
    }

    public void stopPPP(){if(pppProcess!=null) pppProcess.destroy();}
}
