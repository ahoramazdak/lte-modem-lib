package com.example.ltemodem.events;
public class PdpEvent extends ModemEvent {
    private final boolean attached;
    private final boolean activated;
    public PdpEvent(boolean attached,boolean activated){ this.attached = attached;this.activated=activated; }
    public boolean isAttached() {
        return attached;
    }
    public boolean isActivated() {
        return activated;
    }
    @Override public String toString(){
        return "PdpEvent{attached=" + attached + ",activated="+activated+"}";
    }
}
