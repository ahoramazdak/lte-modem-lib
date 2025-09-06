package com.example.ltemodem.events;

/** Raw response line from the modem. */
public final class RawResponseEvent extends ModemEvent {
    private final String raw;

    public RawResponseEvent(final String rawText) {
        this.raw = rawText;
    }

    /**
     * Return the raw modem line.
     *
     * @return raw modem text
     */
    public String getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return "RawResponse{" + raw + "}";
    }
}
