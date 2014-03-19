/*
 * Copyright (C) 2014 Florian Anselstetter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.anselstetter.eventbus;

import net.anselstetter.eventbus.event.Event;
import net.anselstetter.eventbus.transport.EventTransport;
import net.anselstetter.eventbus.transport.InMemoryTransport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An event bus to deliver events to subscribers.
 * <p/>
 * Note: This class does not guarantee uniqueness.
 * You can have as many buses, as you want.
 * Drop it in a singleton, if you need uniqueness.
 */
public class EventBus {

    public static final String DEFAULT_IDENTIFIER = "default";

    private final EventTransport transport;
    private final ThreadEnforcer threadEnforcer;
    private final Thread runningThread;
    private final String identifier;
    private final Map<Class<? extends Event>, List<EventCallback<? extends Event>>> subscribers;
    private final Map<Class<? extends Event>, Event> lastEvent;
    private final Map<String, SubscriptionHolder> taggedSubscriptions;

    /**
     * Constructor
     *
     * @param transport      The transport used for event delivery
     * @param threadEnforcer Thread enforcer
     * @param identifier     Identifier for this event bus {@link #toString}
     */
    public EventBus(EventTransport transport, ThreadEnforcer threadEnforcer, String identifier) {
        this.transport = transport;
        this.threadEnforcer = threadEnforcer;
        this.runningThread = Thread.currentThread();
        this.identifier = identifier;
        this.subscribers = new HashMap<>();
        this.lastEvent = new HashMap<>();
        this.taggedSubscriptions = new HashMap<>();
    }

    /**
     * Register a callback for a specific event
     *
     * @param cls      Lookup class for {@link  #subscribers}
     * @param callback Called when an event with the type cls is posted. See {@link #post(net.anselstetter.eventbus.event.Event)}
     */
    public void register(Class<? extends Event> cls, EventCallback<? extends Event> callback) {
        register(cls, callback, false);
    }

    /**
     * Register a callback for a specific event
     *
     * @param cls              Lookup class for {@link #subscribers}
     * @param callback         Called when an event with the type cls is posted. See {@link #post(net.anselstetter.eventbus.event.Event)}
     * @param deliverLastEvent If the requested event has already been delivered, redeliver it to the subscriber
     */
    @SuppressWarnings("unchecked")
    public void register(Class<? extends Event> cls, EventCallback<? extends Event> callback, boolean deliverLastEvent) {
        register(cls, callback, deliverLastEvent, null);
    }

    /**
     * Register a callback for a specific event
     *
     * @param cls      Lookup class for {@link #subscribers}
     * @param callback Called when an event with the type cls is posted. See {@link #post(net.anselstetter.eventbus.event.Event)}
     * @param tag      A tag for referencing event subscriptions later
     */
    @SuppressWarnings("unchecked")
    public void register(Class<? extends Event> cls, EventCallback<? extends Event> callback, String tag) {
        register(cls, callback, false, tag);
    }

    /**
     * Register a callback for a specific event
     *
     * @param cls              Lookup class for {@link #subscribers}
     * @param callback         Called when an event with the type cls is posted. See {@link #post(net.anselstetter.eventbus.event.Event)}
     * @param deliverLastEvent If the requested event has already been delivered, redeliver it to the subscriber
     * @param tag              A tag for referencing event subscriptions later
     */
    @SuppressWarnings("unchecked")
    public void register(Class<? extends Event> cls, EventCallback<? extends Event> callback, boolean deliverLastEvent, String tag) {
        threadEnforcer.enforce(this);

        if (!subscribers.containsKey(cls)) {
            subscribers.put(cls, new ArrayList<EventCallback<? extends Event>>());
        }

        List<EventCallback<? extends Event>> list = subscribers.get(cls);

        if (!list.contains(callback)) {
            list.add(callback);

            if (tag != null) {
                taggedSubscriptions.put(tag, new SubscriptionHolder(cls, callback));
            }

            if (deliverLastEvent && lastEvent.containsKey(cls)) {
                transport.deliver(lastEvent.get(cls), (EventCallback<Event>) callback);
            }
        } else {
            throw new IllegalStateException("Event bus " + toString() + " already has a registered callback of type: " + callback + " for class: " + cls);
        }
    }

    /**
     * Unregister a callback for a specific event
     *
     * @param cls      Lookup class for {@link  #subscribers}
     * @param callback Called when an event with the type cls is posted. See {@link #post(net.anselstetter.eventbus.event.Event)}
     */
    public void unregister(Class<? extends Event> cls, EventCallback<? extends Event> callback) {
        threadEnforcer.enforce(this);

        if (subscribers.containsKey(cls)) {
            List<EventCallback<? extends Event>> list = subscribers.get(cls);

            if (list.contains(callback)) {
                list.remove(callback);
            } else {
                throw new IllegalStateException("Event bus " + toString() + " does not contain a callback of type: " + callback + " for class: " + cls);
            }
        } else {
            throw new IllegalStateException("Event bus " + toString() + " does not maintain a subscription list of class: " + cls);
        }
    }

    /**
     * Unregister a callback for a specific event
     *
     * @param tag A tag for referencing event subscriptions
     */
    public void unregister(String tag) {
        threadEnforcer.enforce(this);

        SubscriptionHolder holder = null;

        if (hasTaggedSubscriber(tag)) {
            holder = taggedSubscriptions.get(tag);
        }

        if (holder != null) {
            unregister(holder.getEventClass(), holder.getEventCallback());
            taggedSubscriptions.remove(tag);
        } else {
            throw new IllegalStateException("Event bus " + toString() + " does not contain a event subscription matching this tag: " + tag);
        }
    }

    /**
     * Clear all subscribers and event history
     */
    public void reset() {
        subscribers.clear();
        lastEvent.clear();
    }

    /**
     * Notify all subscribers listening to the posted event type
     *
     * @param event Event to deliver to all subscribers
     */
    @SuppressWarnings("unchecked")
    public void post(Event event) {
        threadEnforcer.enforce(this);
        lastEvent.put(event.getClass(), event);

        List<EventCallback<? extends Event>> list = subscribers.get(event.getClass());

        if (list != null) {
            for (EventCallback callback : list) {
                transport.deliver(event, callback);
            }
        }
    }

    /**
     * Get the number of registered subscribers for an event type
     *
     * @param cls Lookup class for {@link  #subscribers}
     */
    public int getSubscriberCountForEvent(Class<? extends Event> cls) {
        if (!subscribers.containsKey(cls)) {
            return 0;
        }

        return subscribers.get(cls).size();
    }

    /**
     * Get the number of registered subscribers for an event type
     *
     * @param tag A tag for referencing event subscriptions
     */
    public boolean hasTaggedSubscriber(String tag) {
        return taggedSubscriptions.containsKey(tag);
    }

    /**
     * Returns the thread, where the bus was instantiated. Can be used in {@link net.anselstetter.eventbus.ThreadEnforcer}
     *
     * @return Current thread
     */
    public Thread getRunningThread() {
        return runningThread;
    }

    @Override
    public String toString() {
        return "[EventBus \"" + identifier + "\"]";
    }

    private class SubscriptionHolder {

        private final Class<? extends Event> eventClass;
        private final EventCallback<? extends Event> eventCallback;

        public SubscriptionHolder(Class<? extends Event> eventClass, EventCallback<? extends Event> eventCallback) {
            this.eventClass = eventClass;
            this.eventCallback = eventCallback;
        }

        public Class<? extends Event> getEventClass() {
            return eventClass;
        }

        public EventCallback<? extends Event> getEventCallback() {
            return eventCallback;
        }
    }

    /**
     * Subscriber callbacks must implement this interface. See {@link #register(Class, net.anselstetter.eventbus.EventBus.EventCallback)}
     * and {@link #unregister(Class, net.anselstetter.eventbus.EventBus.EventCallback)}
     *
     * @param <T> Subclass of Event
     */
    public static interface EventCallback<T> {

        public void onNotify(T event);
    }

    /**
     * Convenience class to instantiate {@link #EventBus(net.anselstetter.eventbus.transport.EventTransport, ThreadEnforcer, String)}
     */
    public static class Builder {

        private EventTransport transport;
        private ThreadEnforcer threadEnforcer;
        private String identifier;

        public Builder setTransport(EventTransport transport) {
            this.transport = transport;

            return this;
        }

        public Builder setThreadEnforcer(ThreadEnforcer threadEnforcer) {
            this.threadEnforcer = threadEnforcer;

            return this;
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;

            return this;
        }

        public EventBus build() {
            if (transport == null) {
                transport = new InMemoryTransport();
            }

            if (threadEnforcer == null) {
                threadEnforcer = ThreadEnforcer.ANY;
            }

            if (identifier == null) {
                identifier = EventBus.DEFAULT_IDENTIFIER;
            }

            return new EventBus(transport, threadEnforcer, identifier);
        }
    }
}
