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
 *         Time: 7:27 PM
 */
public class CallbackTest {

    final String EXPECTED_RESULT = "SUCCESS";

    private class TestEvent extends Event {

        public final String TEST;

        public TestEvent(String test) {
            TEST = test;
        }
    }

    private final EventBus bus = new EventBus.Builder()
            .build();

    private String result;

    @Test
    public void testDelivery() {
        result = null;

        bus.reset();
        bus.register(TestEvent.class, new EventBus.EventCallback<TestEvent>() {
            @Override
            public void onNotify(TestEvent event) {
                result = event.TEST;
            }
        });

        bus.post(new TestEvent(EXPECTED_RESULT));

        assertEquals("result shoud be " + EXPECTED_RESULT, result, EXPECTED_RESULT);
    }

    @Test
    public void testDeliveryWithInitialEvent() {
        bus.reset();
        bus.post(new TestEvent(EXPECTED_RESULT));
        bus.register(TestEvent.class, new EventBus.EventCallback<TestEvent>() {
            @Override
            public void onNotify(TestEvent event) {
                result = event.TEST;
            }
        }, true);

        assertEquals("result shoud be " + EXPECTED_RESULT, result, EXPECTED_RESULT);

        result = null;

        bus.register(TestEvent.class, new EventBus.EventCallback<TestEvent>() {
            @Override
            public void onNotify(TestEvent event) {
                result = event.TEST;
            }
        });

        assertEquals("result shoud be null", result, null);
    }
}
