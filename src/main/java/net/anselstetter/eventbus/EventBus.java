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
import java.util.Collections;
import java.util.Comparator;
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

    private final Map<String, List<CallbackSubscription>> taggedSubscriptions;

    /**
     * Constructor
     *
     * @param transport      The transport used for event delivery
     * @param threadEnforcer Thread enforcer
     * @param identifier     Identifier for this event bus {@link #toString}
     */
    private EventBus(EventTransport transport, ThreadEnforcer threadEnforcer, String identifier) {
        this.transport = transport;
        this.threadEnforcer = threadEnforcer;
        this.runningThread = Thread.currentThread();
        this.identifier = identifier;
        this.subscribers = new HashMap<Class<? extends Event>, List<CallbackSubscription>>();
        this.lastEvent = new HashMap<Class<? extends Event>, Event>();
        this.taggedSubscriptions = new HashMap<String, List<CallbackSubscription>>();
    }

    /**
     * Fluid interface for event registration
     *
     * @param cls Lookup class for {@link  #subscribers}
     * @param <T> Subclass of Event
     * @return EventRegistration
     */
    public <T extends Event> EventRegistration<T> on(Class<T> cls) {
        return new EventRegistration<T>(this, cls);
    }

    /**
     * Register a callback for a specific event
     *
     * @param cls              Lookup class for {@link #subscribers}
     * @param callback         Called when an event with the type cls is posted. See {@link
     *                         #post(net.anselstetter.eventbus.event.Event)}
     * @param deliverLastEvent If the requested event has already been delivered, redeliver it to
     *                         the subscriber
     * @param priority         The priority in which the callback will be invoked
     * @return The EventBus
     */
    private <T extends Event> EventBus register(Class<T> cls, EventCallback<T> callback,
            boolean deliverLastEvent, String tag, int priority) {
        return register(new CallbackSubscription<T>(cls, callback, tag, priority), deliverLastEvent);
    }

    /**
     * Register a callback for a specific event
     *
     * @param subscription Subscription class containing the event, callback, priority and a tag {@link
     *                     net.anselstetter.eventbus.EventBus.CallbackSubscription}
     * @return The EventBus
     */
    private <T extends Event> EventBus register(CallbackSubscription<T> subscription, boolean deliverLastEvent) {
        threadEnforcer.enforce(this);

        Class<T> cls = subscription.getEventClass();
        EventCallback<T> callback = subscription.getEventCallback();
        String tag = subscription.getTag();

        if (!subscribers.containsKey(cls)) {
            subscribers.put(cls, new ArrayList<CallbackSubscription>());
        }

        List<CallbackSubscription> list = subscribers.get(cls);

        if (!list.contains(subscription)) {
            list.add(subscription);

            if (subscription.getTag() != null) {
                if (!taggedSubscriptions.containsKey(subscription.getTag())) {
                    taggedSubscriptions.put(subscription.getTag(), new ArrayList<CallbackSubscription>());
                }

                taggedSubscriptions.get(tag).add(subscription);
            }

            if (deliverLastEvent && lastEvent.containsKey(cls)) {
                deliver(lastEvent.get(cls), subscription);
            }

            sort(list);
        } else {
            throw new IllegalStateException("Event bus " + toString() + " already has a registered callback of type: "
                    + callback + " for class: " + cls
            );
        }

        return this;
    }

    /**
     * Unregister a callback for a specific event
     *
     * @param subscription Subscription class containing the event, callback and a tag {@link
     *                     net.anselstetter.eventbus.EventBus.CallbackSubscription}
     */
    private <T extends Event> void unregister(CallbackSubscription<T> subscription) {
        threadEnforcer.enforce(this);

        Class<T> cls = subscription.getEventClass();
        EventCallback<T> callback = subscription.getEventCallback();
        String tag = subscription.getTag();

        if (subscribers.containsKey(cls)) {
            List<CallbackSubscription> list = subscribers.get(cls);

            if (list.contains(subscription)) {
                list.remove(subscription);

                if (tag != null) {
                    taggedSubscriptions.remove(subscription.getTag());
                }
            } else {
                throw new IllegalStateException(
                        "Event bus " + toString() + " does not contain a callback of type: " + callback + " for class: "
                                + cls
                );
            }
        } else {
            throw new IllegalStateException(
                    "Event bus " + toString() + " does not maintain a subscription list of class: " + cls
            );
        }
    }

    /**
     * Unregister a callback for a specific event
     *
     * @param cls      Lookup class for {@link  #subscribers}
     * @param callback Called when an event with the type cls is posted. See {@link
     *                 #post(net.anselstetter.eventbus.event.Event)}
     */
    private <T extends Event> void unregister(Class<T> cls, EventCallback<T> callback) {
        unregister(new CallbackSubscription<T>(cls, callback, null, 0));
    }

    /**
     * Unregister a callback for a specific event
     *
     * @param tag A tag for referencing event subscriptions
     */
    public void unregister(String tag) {
        List<CallbackSubscription> subscriptions = null;

        if (hasTaggedSubscriber(tag)) {
            subscriptions = taggedSubscriptions.get(tag);
        }

        if (subscriptions != null) {
            for (CallbackSubscription subscription : subscriptions) {
                unregister(subscription);
            }
        } else {
            throw new IllegalStateException(
                    "Event bus " + toString() + " does not contain a event subscription matching this tag: " + tag);
        }
    }

    /**
     * Sort a list by priority in descending order
     *
     * @param list The list to sort
     */
    private void sort(List<CallbackSubscription> list) {
        Collections.sort(list, new Comparator<CallbackSubscription>() {
            @Override
            public int compare(CallbackSubscription subscription, CallbackSubscription subscription2) {
                if (subscription.getPriority() > subscription2.getPriority()) {
                    return -1;
                } else if (subscription.getPriority() < subscription2.getPriority()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    }

    /**
     * Clear all subscribers and event history
     */
    public void reset() {
        subscribers.clear();
        lastEvent.clear();
        taggedSubscriptions.clear();
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

    /**
     * Deliver an event to it's subscriber
     *
     * @param event        The event
     * @param subscription the callback subscription
     */
    private void deliver(Event event, CallbackSubscription subscription) {
        transport.deliver(event, subscription.getEventCallback());
    }

    /**
     * Get the number of registered subscribers for an event type
     *
     * @param cls Lookup class for {@link  #subscribers}
     */
    public <T extends Event> int getSubscriberCountForEvent(Class<T> cls) {
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
     * Returns the thread, where the bus was instantiated. Can be used in {@link
     * net.anselstetter.eventbus.ThreadEnforcer}
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
     * Helper class for fluid event registration
     *
     * @param <T> Subclass of Event
     */
    public static class EventRegistration<T extends Event> {

        private final EventBus bus;

        private final Class<T> cls;

        private String tag;

        private int priority;

        private boolean deliverLastEvent = false;

        private EventRegistration(EventBus bus, Class<T> cls) {
            this.bus = bus;
            this.cls = cls;
        }

        public EventRegistration<T> setTag(String tag) {
            this.tag = tag;

            return this;
        }

        public EventRegistration<T> setPriority(int priority) {
            this.priority = priority;

            return this;
        }

        public EventRegistration<T> deliverLastEvent() {
            this.deliverLastEvent = true;

            return this;
        }

        public EventBus callback(EventCallback<T> callback) {
            bus.register(cls, callback, deliverLastEvent, tag, priority);

            return bus;
        }

        public void unregister(EventCallback<T> callback) {
            bus.unregister(cls, callback);
        }
    }

    /**
     * Convenience class to instantiate {@link #EventBus(net.anselstetter.eventbus.transport.EventTransport,
     * ThreadEnforcer, String)}
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

    /**
     * Holder class to encapsulate an event, callback and tag
     */
    private class CallbackSubscription<T extends Event> {

        private final Class<T> eventClass;

        private final EventCallback<T> eventCallback;

        private final String tag;

        private final int priority;

        private CallbackSubscription(Class<T> eventClass, EventCallback<T> eventCallback, String tag, int priority) {
            this.eventClass = eventClass;
            this.eventCallback = eventCallback;
            this.tag = tag;
            this.priority = priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CallbackSubscription that = (CallbackSubscription) o;

            return eventCallback.equals(that.eventCallback);
        }

        @Override
        public int hashCode() {
            return eventCallback.hashCode();
        }

        public Class<T> getEventClass() {
            return eventClass;
        }

        public EventCallback<T> getEventCallback() {
            return eventCallback;
        }

        public String getTag() {
            return tag;
        }

        public int getPriority() {
            return priority;
        }
    }
}
