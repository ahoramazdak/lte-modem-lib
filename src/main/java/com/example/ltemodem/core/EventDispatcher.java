/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.ltemodem.core;

import com.example.ltemodem.events.ModemEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 *
 * @author amin
 */
public class EventDispatcher {

    private final List<Consumer<ModemEvent>> listeners = new CopyOnWriteArrayList<>();

    public void dispatch(ModemEvent event) {
        listeners.forEach(l -> l.accept(event));
    }

    public void addListener(Consumer<ModemEvent> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<ModemEvent> listener) {
        listeners.remove(listener);
    }
}
