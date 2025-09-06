package com.example.ltemodem.core;
// Lightweight event dispatcher for modem events.

import com.example.ltemodem.events.ModemEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple, thread-safe dispatcher for ModemEvent instances.
 * <p>
 * Listeners are invoked on the caller thread; listener exceptions are
 * logged and swallowed so one bad listener cannot break others.
 */
public final class EventDispatcher {

    /** Registered listeners invoked for each dispatched event. */
    private final List<Consumer<ModemEvent>> listeners = new CopyOnWriteArrayList<>();

    /** Logger for dispatcher errors and diagnostics. */
    private static final Logger LOG = LoggerFactory.getLogger(
            EventDispatcher.class);

    /**
     * Dispatch an event to all registered listeners; listener exceptions are logged and swallowed.
     *
     * @param event the event to dispatch
     */
    public void dispatch(final ModemEvent event) {
        for (final Consumer<ModemEvent> l : listeners) {
            try {
                l.accept(event);
            } catch (final Throwable t) {
                LOG.error("Listener threw while handling event {}", event, t);
            }
        }
    }
    /**
     * Add a listener that will be invoked for future events.
     *
     * @param listener the listener to add
     */
    public void addListener(final Consumer<ModemEvent> listener) {
        listeners.add(listener);
    }

    /**
     * Remove a previously added listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(final Consumer<ModemEvent> listener) {
        listeners.remove(listener);
    }
}
