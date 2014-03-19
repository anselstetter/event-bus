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
    private final Map<Class<? extends Event>, List<CallbackSubscription>> subscribers;
    private final Map<Class<? extends Event>, Event> lastEvent;
    private final Map<String, CallbackSubscription> taggedSubscriptions;

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
        register(new CallbackSubscription(cls, callback), false);
    }

    /**
     * Register a callback for a specific event
     *
     * @param cls              Lookup class for {@link #subscribers}
     * @param callback         Called when an event with the type cls is posted. See {@link #post(net.anselstetter.eventbus.event.Event)}
     * @param deliverLastEvent If the requested event has already been delivered, redeliver it to the subscriber
     */
    public void register(Class<? extends Event> cls, EventCallback<? extends Event> callback, boolean deliverLastEvent) {
        register(new CallbackSubscription(cls, callback), deliverLastEvent);
    }

    /**
     * Register a callback for a specific event
     *
     * @param cls      Lookup class for {@link #subscribers}
     * @param callback Called when an event with the type cls is posted. See {@link #post(net.anselstetter.eventbus.event.Event)}
     * @param tag      A tag for referencing event subscriptions later
     */
    public void register(Class<? extends Event> cls, EventCallback<? extends Event> callback, String tag) {
        register(new CallbackSubscription(cls, callback, tag), false);
    }

    /**
     * Register a callback for a specific event
     *
     * @param cls              Lookup class for {@link #subscribers}
     * @param callback         Called when an event with the type cls is posted. See {@link #post(net.anselstetter.eventbus.event.Event)}
     * @param deliverLastEvent If the requested event has already been delivered, redeliver it to the subscriber
     */
    public void register(Class<? extends Event> cls, EventCallback<? extends Event> callback, boolean deliverLastEvent, String tag) {
        register(new CallbackSubscription(cls, callback, tag), deliverLastEvent);
    }

    /**
     * Register a callback for a specific event
     *
     * @param subscription Subscription class containing the event, callback and a tag {@link net.anselstetter.eventbus.EventBus.CallbackSubscription}
     */
    private void register(CallbackSubscription subscription, boolean deliverLastEvent) {
        threadEnforcer.enforce(this);

        Class<? extends Event> cls = subscription.getEventClass();
        EventCallback<? extends Event> callback = subscription.getEventCallback();
        String tag = subscription.getTag();

        if (!subscribers.containsKey(cls)) {
            subscribers.put(cls, new ArrayList<CallbackSubscription>());
        }

        List<CallbackSubscription> list = subscribers.get(cls);

        if (!list.contains(subscription)) {
            list.add(subscription);

            if (subscription.getTag() != null) {
                taggedSubscriptions.put(tag, subscription);
            }

            if (deliverLastEvent && lastEvent.containsKey(cls)) {
                deliver(lastEvent.get(cls), subscription);
            }
        } else {
            throw new IllegalStateException("Event bus " + toString() + " already has a registered callback of type: " + callback + " for class: " + cls);
        }
    }

    /**
     * Unregister a callback for a specific event
     *
     * @param subscription Subscription class containing the event, callback and a tag {@link net.anselstetter.eventbus.EventBus.CallbackSubscription}
     */
    private void unregister(CallbackSubscription subscription) {
        threadEnforcer.enforce(this);

        Class<? extends Event> cls = subscription.getEventClass();
        EventCallback<? extends Event> callback = subscription.getEventCallback();
        String tag = subscription.getTag();

        if (subscribers.containsKey(cls)) {
            List<CallbackSubscription> list = subscribers.get(cls);

            if (list.contains(subscription)) {
                list.remove(subscription);

                if (tag != null) {
                    taggedSubscriptions.remove(subscription.getTag());
                }
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
     * @param cls      Lookup class for {@link  #subscribers}
     * @param callback Called when an event with the type cls is posted. See {@link #post(net.anselstetter.eventbus.event.Event)}
     */
    public void unregister(Class<? extends Event> cls, EventCallback<? extends Event> callback) {
        threadEnforcer.enforce(this);

        unregister(new CallbackSubscription(cls, callback));
    }

    /**
     * Unregister a callback for a specific event
     *
     * @param tag A tag for referencing event subscriptions
     */
    public void unregister(String tag) {
        threadEnforcer.enforce(this);

        CallbackSubscription subscription = null;

        if (hasTaggedSubscriber(tag)) {
            subscription = taggedSubscriptions.get(tag);
        }

        if (subscription != null) {
            unregister(subscription);
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

    public void post(Event event) {
        threadEnforcer.enforce(this);
        lastEvent.put(event.getClass(), event);

        List<CallbackSubscription> list = subscribers.get(event.getClass());

        if (list != null) {
            for (CallbackSubscription subscription : list) {
                deliver(event, subscription);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void deliver(Event event, CallbackSubscription subscription) {
        transport.deliver(event, (EventCallback<Event>) subscription.getEventCallback());
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

    /**
     * Holder class to encapsulate an event, callback and tag
     */
    private class CallbackSubscription {

        private final Class<? extends Event> eventClass;
        private final EventCallback<? extends Event> eventCallback;
        private final String tag;

        private CallbackSubscription(Class<? extends Event> eventClass, EventCallback<? extends Event> eventCallback) {
            this(eventClass, eventCallback, null);
        }

        private CallbackSubscription(Class<? extends Event> eventClass, EventCallback<? extends Event> eventCallback, String tag) {
            this.eventClass = eventClass;
            this.eventCallback = eventCallback;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CallbackSubscription that = (CallbackSubscription) o;

            return eventCallback.equals(that.eventCallback);
        }

        @Override
        public int hashCode() {
            return eventCallback.hashCode();
        }

        public Class<? extends Event> getEventClass() {
            return eventClass;
        }

        public EventCallback<? extends Event> getEventCallback() {
            return eventCallback;
        }

        public String getTag() {
            return tag;
        }
    }

    /**
     * Subscriber callbacks must implement this interface. See {@link #register(Class, net.anselstetter.eventbus.EventBus.EventCallback)}
     * and {@link #unregister(Class, net.anselstetter.eventbus.EventBus.EventCallback)}
     *
     * @param <T> Subclass of Event
     */
    public static interface EventCallback<T extends Event> {

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
