package com.example.ltemodem.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
// no event imports required in this API class

/**
 * High-level modem API that sends AT commands via an underlying
 * {@link LteModemManager} and parses common responses.
 */
public final class LteModemApi {
    /** Short blocking timeout for simple commands (ms). */
    private static final int TIMEOUT_SHORT_MS = 3000;

    /** Medium blocking timeout (ms). */
    private static final int TIMEOUT_MEDIUM_MS = 5000;

    /** Long blocking timeout (ms). */
    private static final int TIMEOUT_LONG_MS = 10000;

    /** Underlying modem manager used to perform IO. */
    private final LteModemManager modem;

    /** Regex used to extract USSD decoded payload from modem response. */
    private static final String USSD_REGEX = "\\+CUSD:\\s*\\d+,\"([^\"]+)\",(\\d+)";

    /** Regex used to extract the IP address from +CGPADDR response. */
    private static final String CGPADDR_REGEX = "\\+CGPADDR:\\s*1,(\\S+)";

    /**
     * Create a new API backed by the provided modem manager.
     *
     * @param modemManager the underlying modem manager used to send AT commands
     */
    public LteModemApi(final LteModemManager modemManager) {
        this.modem = modemManager;
    }

    /**
     * Return the modem identification string (ATI).
     *
     * @return the ATI response from the modem
     * @throws Exception on communication errors
     */
    public String getModemInfo() throws Exception {
        return modem.sendAndWait("ATI", TIMEOUT_SHORT_MS);
    }

    /**
     * Send an SMS (simple blocking sequence).
     *
     * @param number recipient phone number
     * @param text text payload
     * @return true when the modem replies OK to the send operation
     * @throws Exception on communication errors
     */
    public boolean sendSms(final String number, final String text) throws Exception {
        modem.sendAndWait("AT+CMGF=1", TIMEOUT_SHORT_MS);
        modem.sendAndWait("AT+CMGS=\"" + number + "\"", TIMEOUT_SHORT_MS);
        modem.sendCommand(text + "\u001A");
        return modem.sendAndWait("", TIMEOUT_MEDIUM_MS).contains("OK");
    }

    /**
     * Enable unsolicited USSD notifications.
     *
     * @return true when the modem acknowledges the command
     * @throws Exception on communication errors
     */
    public boolean enableUssdNotifications() throws Exception {
        return modem.sendAndWait("AT+CUSD=1", TIMEOUT_SHORT_MS).contains("OK");
    }

    /**
     * Send a USSD code and return the decoded response or the raw payload.
     *
     * @param code the USSD string to send (e.g. *123#)
     * @param dcs data coding scheme value
     * @return decoded USSD response, or the raw modem payload when decoding fails
     * @throws Exception on communication errors
     */
    public String sendUssd(final String code, final int dcs) throws Exception {
    final String cmd = "AT+CUSD=1,\"" + code + "\"," + dcs;
    final String resp = modem.sendAndWait(cmd, TIMEOUT_LONG_MS);
    final Pattern p = Pattern.compile(USSD_REGEX);
    final Matcher m = p.matcher(resp);
    return m.find() ? m.group(1) : resp;
    }

    // ------------------ PDP / Data Session ------------------

    /**
     * Configure and attach the PDP context using the provided APN.
     *
     * @param apn access point name to configure
     * @return true when attach succeeded
     * @throws Exception on communication errors
     */
    public boolean attachPdpContext(final String apn) throws Exception {
        String setCmd = "AT+CGDCONT=1,\"IP\",\"" + apn + "\"";
        if (!modem.sendAndWait(setCmd, TIMEOUT_SHORT_MS).contains("OK")) {
            return false;
        }
        return modem.sendAndWait("AT+CGATT=1", TIMEOUT_MEDIUM_MS).contains("OK");
    }

    /** Detach the PDP context. */
    public boolean detachPdpContext() throws Exception {
        return modem.sendAndWait("AT+CGATT=0", TIMEOUT_MEDIUM_MS).contains("OK");
    }

    /** Activate the configured PDP context. */
    public boolean activatePdpContext() throws Exception {
        return modem.sendAndWait("AT+CGACT=1,1", TIMEOUT_MEDIUM_MS).contains("OK");
    }

    /** Deactivate the PDP context. */
    public boolean deactivatePdpContext() throws Exception {
        return modem.sendAndWait("AT+CGACT=0,1", TIMEOUT_MEDIUM_MS).contains("OK");
    }

    /**
     * Query the active IP address for PDP context 1, or null if none.
     *
     * @return IP address string or null when not available
     * @throws Exception on communication errors
     */
    public String getIpAddress() throws Exception {
    final String resp = modem.sendAndWait("AT+CGPADDR=1", TIMEOUT_SHORT_MS);
    final Pattern p = Pattern.compile(CGPADDR_REGEX);
    final Matcher m = p.matcher(resp);
    return m.find() ? m.group(1) : null;
    }

    /**
     * Return true when the modem reports it is attached to the network.
     *
     * @return true when modem is attached
     * @throws Exception on communication errors
     */
    public boolean isAttached() throws Exception {
        return modem.sendAndWait("AT+CGATT?", TIMEOUT_SHORT_MS).contains("1");
    }
}
