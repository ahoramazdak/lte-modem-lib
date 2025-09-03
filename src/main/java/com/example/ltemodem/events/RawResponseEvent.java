package com.example.ltemodem.events;
public class RawResponseEvent extends ModemEvent {
    private final String raw;
    public RawResponseEvent(String raw){ this.raw = raw; }
    public String getRaw(){ return raw; }
    @Override public String toString(){ return "RawResponse{" + raw + "}"; }
}
