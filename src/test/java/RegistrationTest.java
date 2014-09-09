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

import net.anselstetter.eventbus.EventBus;
import net.anselstetter.eventbus.EventCallback;
import net.anselstetter.eventbus.event.Event;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class RegistrationTest {

    private final String TAG = "TEST";

    private final EventCallback<NopeEvent> callback2 = new EventCallback<NopeEvent>() {
        @Override
        public void onEvent(NopeEvent event) {

        }
    };

    private final EventCallback<TestEvent> callback = new EventCallback<TestEvent>() {
        @Override
        public void onEvent(TestEvent event) {

        }
    };

    private final EventBus bus = new EventBus.Builder()
            .build();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @After
    public void reset() {
        bus.reset();
    }

    @Test
    public void testDoubleRegistrationShouldFail() {
        bus
                .on(TestEvent.class)
                .callback(callback);

        exception.expect(IllegalStateException.class);

        bus
                .on(TestEvent.class)
                .callback(callback);
    }

    @Test
    public void testDoubleUnRegistrationShouldFail() {
        bus
                .on(TestEvent.class)
                .callback(callback);
        bus
                .on(TestEvent.class)
                .unregister(callback);

        exception.expect(IllegalStateException.class);

        bus
                .on(TestEvent.class)
                .unregister(callback);
    }

    @Test
    public void testRegistrationViaTagShouldPass() {
        bus
                .on(TestEvent.class)
                .setTag(TAG)
                .callback(callback);

        assertEquals("subscriber tag error", bus.hasTaggedSubscriber(TAG), true);
        assertEquals("unregister error", 1, bus.getSubscriberCountForEvent(TestEvent.class));
    }

    @Test
    public void testUnRegistrationViaExistingTagShouldPass() {
        bus
                .on(TestEvent.class)
                .setTag(TAG)
                .callback(callback);
        bus
                .on(NopeEvent.class)
                .setTag(TAG)
                .callback(callback2);

        bus.unregister(TAG);

        assertEquals("subscriber tag error", false, bus.hasTaggedSubscriber(TAG));
        assertEquals("unregister error", 0, bus.getSubscriberCountForEvent(TestEvent.class));
    }

    @Test
    public void testUnRegistrationViaNonExistingTagShouldFail() {
        bus
                .on(TestEvent.class)
                .setTag(TAG)
                .callback(callback);

        exception.expect(IllegalStateException.class);

        bus.unregister("NOPE");
    }

    @Test
    public void testUnRegistrationForExistingEventTypeShouldPass() {
        bus
                .on(TestEvent.class)
                .callback(callback);
        bus
                .on(TestEvent.class)
                .unregister(callback);

        assertEquals("unregister error", 0, bus.getSubscriberCountForEvent(TestEvent.class));
    }

    @Test
    public void testUnRegistrationForMissingEventTypeShouldFail() {
        bus
                .on(TestEvent.class)
                .callback(callback);

        exception.expect(IllegalStateException.class);

        bus
                .on(NopeEvent.class)
                .unregister(callback2);
    }

    private class TestEvent extends Event {

    }

    private class NopeEvent extends Event {

    }
}
