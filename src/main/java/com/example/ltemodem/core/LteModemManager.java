package com.example.ltemodem.core;

import com.example.ltemodem.events.*;
import com.fazecast.jSerialComm.SerialPort;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LteModemManager {
    private SerialPort serialPort;
    private InputStream in;
    private OutputStream out;
    private volatile boolean running = false;
    private Thread readerThread;
//    private final List<ModemEventListener> listeners = new ArrayList<>();
    private EventDispatcher dispatcher=new EventDispatcher();
    private final StringBuilder responseBuffer = new StringBuilder();
    private static final Logger log = LoggerFactory.getLogger(LteModemManager.class);
    public LteModemManager() {}
    public LteModemManager(InputStream in, OutputStream out) {
        this.in = in; this.out = out;
        startReaderThread();
    }

    public boolean connect(String portName, int baudRate) {
        if (in != null && out != null) return true;
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
        if (!serialPort.openPort()) { 
            log.error("Failed to open port "+portName); return false; }
        in = serialPort.getInputStream();
        out = serialPort.getOutputStream();
        startReaderThread();
        return true;
    }

    public void disconnect() { running=false; try{Thread.sleep(100);}catch(Exception ignored){} if(serialPort!=null && serialPort.isOpen()) serialPort.closePort(); }

    public void addListener(ModemEventListener listener) { dispatcher.addListener((Consumer<ModemEvent>) listener); }
    public void removeListener(ModemEventListener listener) { 
        dispatcher.removeListener((Consumer<ModemEvent>) listener);
    }

    public void sendCommand(String cmd) throws IOException { out.write((cmd+"\r").getBytes()); out.flush(); }

    public String sendAndWait(String cmd,long timeoutMs) throws Exception {
        synchronized(responseBuffer){responseBuffer.setLength(0);}
        sendCommand(cmd);
        long start=System.currentTimeMillis();
        while(System.currentTimeMillis()-start<timeoutMs){
            synchronized(responseBuffer){
                if(responseBuffer.toString().contains("OK") || responseBuffer.toString().contains("ERROR")) return responseBuffer.toString();
            }
            Thread.sleep(50);
        }
        return responseBuffer.toString();
    }

    private void startReaderThread() {
        running=true;
        readerThread = new Thread(() -> {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(in))){
                String line;
                while(running && (line=reader.readLine())!=null){
                    if(line.startsWith("+CUSD:")) handleUssd(line);
                    else if(line.startsWith("+CMTI:")) handleSms(line);
                    else if(line.startsWith("+CGATT:") || line.startsWith("+CGEV:")) handlePdpEvents(line);
                    else { dispatchEvent(new RawResponseEvent(line)); synchronized(responseBuffer){responseBuffer.append(line).append("\n"); } }
                }
            }catch(Exception e){e.printStackTrace();}
        });
        readerThread.start();
    }

    private void handleUssd(String line){
        Pattern p=Pattern.compile("\\+CUSD:\\s*(\\d+),\"([^\"]*)\",(\\d+)");
        Matcher m=p.matcher(line);
        if(m.find()) dispatchEvent(new UssdEvent(Integer.parseInt(m.group(1)), m.group(2), Integer.parseInt(m.group(3))));
        else dispatchEvent(new RawResponseEvent(line));
    }

    private void handleSms(String line){
        Pattern p=Pattern.compile("\\+CMTI:\\s*\"(\\w+)\",(\\d+)");
        Matcher m=p.matcher(line);
        if(m.find()) dispatchEvent(new SmsReceivedEvent(Integer.parseInt(m.group(2)), m.group(1)));
        else dispatchEvent(new RawResponseEvent(line));
    }

    private void handlePdpEvents(String line){
        if(line.startsWith("+CGATT:")) dispatchEvent(new PdpEvent(line.contains("1"),true));
        else if(line.startsWith("+CGEV:")){
            if(line.contains("PDN ACT")) dispatchEvent(new PdpEvent(true,true));
            else if(line.contains("PDN DEACT")) dispatchEvent(new PdpEvent(true,false));
        }
    }

    private void dispatchEvent(ModemEvent event){dispatcher.dispatch(event);}
}
