package com.example.ltemodem.events;

/** Event indicating a new SMS was received and stored. */
public final class SmsReceivedEvent extends ModemEvent {
    /** Storage name where SMS was written. */
    private final String storage;

    /** Index of the message in storage. */
    private final int index;

    public SmsReceivedEvent(final int indexValue, final String storageName) {
        this.storage = storageName;
        this.index = indexValue;
    }

    /**
     * Storage name where SMS was written.
     *
     * @return storage name
     */
    public String getStorage() {
        return storage;
    }

    /**
     * Index of the message in storage.
     *
     * @return message index
     */
    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "SmsReceivedEvent{storage=" + storage + ", index=" + index + "}";
    }
}
