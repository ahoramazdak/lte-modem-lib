package com.example.ltemodem.events;
public class SmsReceivedEvent extends ModemEvent {
    private final String storage;
    private final int index;
    public SmsReceivedEvent(int index, String storage){
        this.storage = storage;
        this.index = index;
    }
    public String getStorage(){ return storage; }
    public int getIndex(){ return index; }
    @Override public String toString(){
        return "SmsReceivedEvent{storage=" + storage + ", index=" + index + "}";
    }
}
