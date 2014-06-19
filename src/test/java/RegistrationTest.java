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
import net.anselstetter.eventbus.event.Event;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Anselstetter
 *         Date: 3/15/14
 *         Time: 7:07 PM
 */
public class RegistrationTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private final String TAG = "TEST";

    private class TestEvent extends Event {

    }

    private class NopeEvent extends Event {

    }

    private final EventBus.EventCallback<NopeEvent> callback2 = new EventBus.EventCallback<NopeEvent>() {
        @Override
        public void onNotify(NopeEvent event) {

        }
    };

    private final EventBus.EventCallback<TestEvent> callback = new EventBus.EventCallback<TestEvent>() {
        @Override
        public void onNotify(TestEvent event) {

        }
    };

    private final EventBus bus = new EventBus.Builder()
            .build();

    @Test
    public void testDoubleRegistrationShouldFail() {
        bus.reset();
        bus.register(TestEvent.class, callback);

        exception.expect(IllegalStateException.class);
        bus.register(TestEvent.class, callback);
    }

    @Test
    public void testDoubleUnRegistrationShouldFail() {
        bus.reset();
        bus.register(TestEvent.class, callback);
        bus.unregister(TestEvent.class, callback);

        exception.expect(IllegalStateException.class);
        bus.unregister(TestEvent.class, callback);
    }

    @Test
    public void testRegistrationViaTagShouldPass() {
        bus.reset();
        bus.register(TestEvent.class, callback, TAG);
        assertEquals("subscriber tag error", bus.hasTaggedSubscriber(TAG), true);
        assertEquals("unregister error", 1, bus.getSubscriberCountForEvent(TestEvent.class));
    }

    @Test
    public void testUnRegistrationViaExistingTagShouldPass() {
        bus.reset();
        bus.register(TestEvent.class, callback, TAG);
		bus.register(NopeEvent.class,callback2, TAG);
        bus.unregister(TAG);

        assertEquals("subscriber tag error", false, bus.hasTaggedSubscriber(TAG));
        assertEquals("unregister error", 0, bus.getSubscriberCountForEvent(TestEvent.class));
    }

    @Test
    public void testUnRegistrationViaNonExistingTagShouldFail() {
        bus.reset();
        bus.register(TestEvent.class, callback, TAG);

        exception.expect(IllegalStateException.class);
        bus.unregister("NOPE");
    }

    @Test
    public void testUnRegistrationForExistingEventTypeShouldPass() {
        bus.reset();
        bus.register(TestEvent.class, callback);
        bus.unregister(TestEvent.class, callback);

        assertEquals("unregister error", 0, bus.getSubscriberCountForEvent(TestEvent.class));
    }

    @Test
    public void testUnRegistrationForMissingEventTypeShouldFail() {
        bus.reset();
        bus.register(TestEvent.class, callback);

        exception.expect(IllegalStateException.class);
        bus.unregister(NopeEvent.class, callback);
    }
}
