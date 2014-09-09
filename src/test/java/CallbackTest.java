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
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CallbackTest {

    final String EXPECTED_RESULT = "SUCCESS";

    private final EventBus bus = new EventBus.Builder()
            .build();

    private String result;

    @After
    public void reset() {
        bus.reset();
    }

    @Test
    public void testHighestPriorityShouldDeliverFirst() {
        final List<Integer> priorities = new ArrayList<Integer>();

        bus
                .on(TestEvent.class)
                .setPriority(100)
                .callback(new EventCallback<TestEvent>() {
                    @Override
                    public void onEvent(TestEvent event) {
                        priorities.add(100);
                    }
                });

        bus
                .on(TestEvent.class)
                .setPriority(10)
                .callback(new EventCallback<TestEvent>() {
                    @Override
                    public void onEvent(TestEvent event) {
                        priorities.add(10);
                    }
                });

        bus
                .on(TestEvent.class)
                .setPriority(1000)
                .callback(new EventCallback<TestEvent>() {
                    @Override
                    public void onEvent(TestEvent event) {
                        priorities.add(1000);
                    }
                });

        bus.post(new TestEvent("test"));

        assertEquals("result should be 1000", 1000, (int) priorities.get(0));
        assertEquals("result should be 100", 100, (int) priorities.get(1));
        assertEquals("result should be 10", 10, (int) priorities.get(2));
    }

    @Test
    public void testDelivery() {
        result = null;

        bus
                .on(TestEvent.class)
                .callback(new EventCallback<TestEvent>() {
            @Override
            public void onEvent(TestEvent event) {
                result = event.TEST;
            }
        });

        bus.post(new TestEvent(EXPECTED_RESULT));

        assertEquals("result should be " + EXPECTED_RESULT, EXPECTED_RESULT, result);
    }

    @Test
    public void testDeliveryWithInitialEvent() {
        bus.post(new TestEvent(EXPECTED_RESULT));
        bus
                .on(TestEvent.class)
                .deliverLastEvent()
                .callback(new EventCallback<TestEvent>() {
                    @Override
                    public void onEvent(TestEvent event) {
                        result = event.TEST;
                    }
                });

        assertEquals("result should be " + EXPECTED_RESULT, EXPECTED_RESULT, result);

        result = null;

        bus
                .on(TestEvent.class)
                .callback(new EventCallback<TestEvent>() {
            @Override
            public void onEvent(TestEvent event) {
                result = event.TEST;
            }
        });

        assertEquals("result should be null", null, result);
    }

    private class TestEvent extends Event {

        public final String TEST;

        public TestEvent(String test) {
            TEST = test;
        }
    }
}
