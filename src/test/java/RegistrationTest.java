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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Anselstetter
 *         Date: 3/15/14
 *         Time: 7:07 PM
 */
public class RegistrationTest {

    private class TestEvent extends Event {

    }

    private final EventBus.EventCallback<TestEvent> callback = new EventBus.EventCallback<TestEvent>() {
        @Override
        public void onNotify(TestEvent event) {
            // TODO Auto-generated method stub
        }
    };

    private final EventBus bus = new EventBus.Builder()
            .build();

    @Test
    public void testSubscriberShouldBeUnique() {
        bus.register(TestEvent.class, callback);
        bus.register(TestEvent.class, callback);

        assertEquals("subscriber count error", bus.getSubscriberCountForEvent(TestEvent.class), 1);
    }

    @Test
    public void testUnregister() {
        bus.register(TestEvent.class, callback);
        bus.unregister(TestEvent.class, callback);

        assertEquals("unregister error", bus.getSubscriberCountForEvent(TestEvent.class), 0);
    }
}
