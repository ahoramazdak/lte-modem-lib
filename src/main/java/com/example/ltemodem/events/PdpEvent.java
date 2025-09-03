package com.example.ltemodem.events;
public class PdpEvent extends ModemEvent {
    private final boolean attached;
    public PdpEvent(boolean attached){ this.attached = attached; }
    public boolean isAttached(){ return attached; }
    @Override public String toString(){
        return "PdpEvent{attached=" + attached + "}";
    }
}
