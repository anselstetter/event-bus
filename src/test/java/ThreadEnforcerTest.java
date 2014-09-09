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
import net.anselstetter.eventbus.ThreadEnforcer;
import net.anselstetter.eventbus.event.Event;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ThreadEnforcerTest {

    private final EventCallback<TestEvent> callback = new EventCallback<TestEvent>() {
        @Override
        public void onEvent(TestEvent event) {

        }
    };

    private final EventBus bus = new EventBus.Builder()
            .setThreadEnforcer(ThreadEnforcer.SAME)
            .build();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @After
    public void reset() {
        bus.reset();
    }

    @Test
    public void testRegisterOnDifferentThreadShouldFail() {
        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run() {
                bus
                        .on(TestEvent.class)
                        .callback(callback);

                exception.expect(IllegalStateException.class);
            }
        });
        newThread.start();
    }

    @Test
    public void testRegisterOnSameThreadShouldPass() {
        bus
                .on(TestEvent.class)
                .callback(callback);
    }

    @Test
    public void testUnregisterOnDifferentThreadShouldFail() {
        bus
                .on(TestEvent.class)
                .callback(callback);

        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run() {
                bus
                        .on(TestEvent.class)
                        .unregister(callback);

                exception.expect(IllegalStateException.class);
            }
        });
        newThread.start();
    }

    @Test
    public void testUnregisterOnSameThreadShouldPass() {
        bus
                .on(TestEvent.class)
                .callback(callback);
        bus
                .on(TestEvent.class)
                .unregister(callback);
    }

    @Test
    public void testPostOnDifferentThreadShouldFail() {
        bus
                .on(TestEvent.class)
                .callback(callback);

        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run() {
                bus.post(new TestEvent());

                exception.expect(IllegalStateException.class);
            }
        });
        newThread.start();
    }

    @Test
    public void testPostOnSameThreadShouldPass() {
        bus
                .on(TestEvent.class)
                .callback(callback);

        bus.post(new TestEvent());
    }

    private class TestEvent extends Event {

    }
}
