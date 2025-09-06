package com.example.ltemodem.events;

/**
 * Event representing an unsolicited USSD notification from the modem.
 */
public final class UssdEvent extends ModemEvent {
    /** USSD status code. */
    private final int status;

    /** Decoded USSD message text. */
    private final String message;

    /** Data coding scheme value. */
    private final int dcs;

    public UssdEvent(final int statusValue, final String messageText, final int dcsValue) {
        this.status = statusValue;
        this.message = messageText;
        this.dcs = dcsValue;
    }

    /**
     * Return the USSD status code.
     *
     * @return status numeric code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Return the decoded USSD message.
     *
     * @return message text
     */
    public String getMessage() {
        return message;
    }

    /**
     * Return the data coding scheme value.
     *
     * @return DCS numeric value
     */
    public int getDcs() {
        return dcs;
    }

    @Override
    public String toString() {
        return "UssdEvent{status=" + status + ", msg=" + message + ", dcs=" + dcs + "}";
    }
}
