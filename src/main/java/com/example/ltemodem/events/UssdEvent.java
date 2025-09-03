package com.example.ltemodem.events;
public class UssdEvent extends ModemEvent {
    private final int status;
    private final String message;
    private final int dcs;
    public UssdEvent(int status, String message, int dcs){
        this.status = status;
        this.message = message;
        this.dcs = dcs;
    }
    public int getStatus(){ return status; }
    public String getMessage(){ return message; }
    public int getDcs(){ return dcs; }
    @Override public String toString(){
        return "UssdEvent{status=" + status + ", msg= + message + , dcs=" + dcs + "}";
    }
}
