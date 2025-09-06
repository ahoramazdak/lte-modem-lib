package com.example.ltemodem.events;

/** Event representing PDP attach/activation state changes. */
public final class PdpEvent extends ModemEvent {
    /** True when modem reports CGATT=1. */
    private final boolean attached;

    /** True when PDP context is active. */
    private final boolean activated;

    public PdpEvent(final boolean attachedFlag, final boolean activatedFlag) {
        this.attached = attachedFlag;
        this.activated = activatedFlag;
    }

    /** Return true when attached to network. */
    public boolean isAttached() {
        return attached;
    }

    /** Return true when PDP context is active. */
    public boolean isActivated() {
        return activated;
    }

    @Override
    public String toString() {
        return "PdpEvent{attached=" + attached + ",activated=" + activated + "}";
    }
}
