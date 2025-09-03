package com.example.ltemodem.mock;
import java.io.*;
public class MockSerialPort {
    private final PipedInputStream in=new PipedInputStream();
    private final PipedOutputStream out=new PipedOutputStream();
    public MockSerialPort() throws IOException { out.connect(in); }
    public InputStream getInputStream(){return in;}
    public OutputStream getOutputStream(){return out;}
    public void simulateIncomingLine(String line) throws IOException { out.write((line+"\r\n").getBytes()); out.flush(); }
}
