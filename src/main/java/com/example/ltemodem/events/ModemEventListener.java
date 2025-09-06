package com.example.ltemodem.events;

/** Listener interface for modem events. */
public interface ModemEventListener {
    /**
     * Handle an incoming modem event.
     *
     * @param event the event to handle
     */
    void onEvent(ModemEvent event);
}
