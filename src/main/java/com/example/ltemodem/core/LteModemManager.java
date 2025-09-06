package com.example.ltemodem.core;

import com.example.ltemodem.events.ModemEvent;
import com.example.ltemodem.events.PdpEvent;
import com.example.ltemodem.events.RawResponseEvent;
import com.example.ltemodem.events.SmsReceivedEvent;
import com.example.ltemodem.events.UssdEvent;
import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LteModemManager {
    /** Underlying serial port when connected. */
    private SerialPort serialPort;

    /** Input stream from the serial port or injected stream. */
    private InputStream in;

    /** Output stream to write AT commands to the modem. */
    private OutputStream out;

    /** Reader thread lifecycle flag. */
    private volatile boolean running = false;

    /** Background thread that reads modem lines. */
    private Thread readerThread;
    /** Dispatcher for publishing parsed modem events to listeners. */
    private final EventDispatcher dispatcher = new EventDispatcher();
    /** Optional reactive dispatcher for Flux-based consumers. */
    private final ReactiveEventDispatcher reactiveDispatcher = new ReactiveEventDispatcher();
    private final StringBuilder responseBuffer = new StringBuilder();
    private static final Logger LOG = LoggerFactory.getLogger(LteModemManager.class);
    private static final int DATA_BITS = 8;
    private static final long DISCONNECT_WAIT_MS = 100L;
    private static final long POLL_INTERVAL_MS = 50L;
    public LteModemManager() {
    }

    public LteModemManager(final InputStream inputStream, final OutputStream outputStream) {
        this.in = inputStream;
        this.out = outputStream;
        startReaderThread();
    }

    /**
     * Open and initialize the serial port.
     *
     * @param portName serial device name
     * @param baudRate baud rate to configure
     * @return true when connection established or already present
     */
    public boolean connect(final String portName, final int baudRate) {
        if (in != null && out != null) {
            return true;
        }
    serialPort = SerialPort.getCommPort(portName);
    serialPort.setBaudRate(baudRate);
    serialPort.setNumDataBits(DATA_BITS);
        serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        serialPort.setParity(SerialPort.NO_PARITY);
        if (!serialPort.openPort()) {
            LOG.error("Failed to open port " + portName);
            return false;
        }
        in = serialPort.getInputStream();
        out = serialPort.getOutputStream();
        startReaderThread();
        return true;
    }

    /**
     * Stop the reader and close the serial port if open.
     */
    public void disconnect() {
        running = false;
        try {
            Thread.sleep(DISCONNECT_WAIT_MS);
        } catch (Exception ignored) {
            // ignore interrupt during shutdown wait
        }
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }

    // // Updated methods to use the correct method name from ModemEventListener
    // public void addListener(ModemEventListener listener) {
    //     Consumer<ModemEvent> c = event -> listener.onEvent(event);
    //     listenerMap.put(listener, c);
    //     dispatcher.addListener(c);
    // }

    // public void removeListener(ModemEventListener listener) {
    //     Consumer<ModemEvent> c = listenerMap.remove(listener);
    //     if (c != null) dispatcher.removeListener(c);
    // }

    // Convenience overloads for direct Consumer listeners
    /**
     * Register a listener for modem events.
     *
     * @param consumer listener to add
     */
    public void addListener(final Consumer<ModemEvent> consumer) {
        dispatcher.addListener(consumer);
    }

    /**
     * Remove a previously registered listener.
     *
     * @param consumer listener to remove
     */
    public void removeListener(final Consumer<ModemEvent> consumer) {
        dispatcher.removeListener(consumer);
    }

    /**
     * Send a raw AT command (automatically appends CR).
     *
     * @param cmd command without CR
     * @throws IOException when write fails
     */
    public void sendCommand(final String cmd) throws IOException {
        out.write((cmd + "\r").getBytes());
        out.flush();
    }

    /**
     * Send a command and wait for an OK/ERROR response or timeout.
     *
     * @param cmd command to send
     * @param timeoutMs timeout in milliseconds
     * @return collected response text
     * @throws Exception on IO or timeout
     */
    public String sendAndWait(final String cmd, final long timeoutMs) throws Exception {
        synchronized (responseBuffer) {
            responseBuffer.setLength(0);
        }

        sendCommand(cmd);
        final long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            synchronized (responseBuffer) {
                final String rb = responseBuffer.toString();
                if (rb.contains("OK") || rb.contains("ERROR")) {
                    return rb;
                }
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }

        return responseBuffer.toString();
    }

    private void startReaderThread() {
        running = true;
        readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                        while (running && (line = reader.readLine()) != null) {
                            if (line.startsWith("+CUSD:")) {
                                handleUssd(line);
                            } else if (line.startsWith("+CMTI:")) {
                                handleSms(line);
                            } else if (line.startsWith("+CGATT:") || line.startsWith("+CGEV:")) {
                                handlePdpEvents(line);
                            } else {
                                dispatchEvent(new RawResponseEvent(line));
                                synchronized (responseBuffer) {
                                    responseBuffer.append(line).append("\n");
                                }
                            }
                        }
            } catch (Exception e) {
                LOG.error("Reader thread encountered an exception", e);
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void handleUssd(final String line) {
        final Pattern p = Pattern.compile("\\+CUSD:\\s*(\\d+),\"([^\"]*)\",(\\d+)");
        final Matcher m = p.matcher(line);
        if (m.find()) {
            dispatchEvent(new UssdEvent(Integer.parseInt(m.group(1)), m.group(2), Integer.parseInt(m.group(3))));
        } else {
            dispatchEvent(new RawResponseEvent(line));
        }
    }

    private void handleSms(final String line) {
        final Pattern p = Pattern.compile("\\+CMTI:\\s*\"(\\w+)\",(\\d+)");
        final Matcher m = p.matcher(line);
        if (m.find()) {
            dispatchEvent(new SmsReceivedEvent(Integer.parseInt(m.group(2)), m.group(1)));
        } else {
            dispatchEvent(new RawResponseEvent(line));
        }
    }

    private void handlePdpEvents(final String line) {
        if (line.startsWith("+CGATT:")) {
            dispatchEvent(new PdpEvent(line.contains("1"), true));
        } else if (line.startsWith("+CGEV:")) {
            if (line.contains("PDN ACT")) {
                dispatchEvent(new PdpEvent(true, true));
            } else if (line.contains("PDN DEACT")) {
                dispatchEvent(new PdpEvent(true, false));
            }
        }
    }

    private void dispatchEvent(final ModemEvent event) {
        dispatcher.dispatch(event);
        reactiveDispatcher.emit(event);
    }

    /**
     * Test helper: emit an event as if received from the modem. Intended for tests only.
     */
    public void testEmitEvent(final ModemEvent event) {
        if (event == null) {
            return;
        }
        dispatchEvent(event);
    }

    /**
     * Return a Flux stream of modem events for reactive consumers.
     */
    public reactor.core.publisher.Flux<ModemEvent> eventFlux() {
        return reactiveDispatcher.flux();
    }
}
