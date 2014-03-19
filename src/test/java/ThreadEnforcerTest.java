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
import net.anselstetter.eventbus.ThreadEnforcer;
import net.anselstetter.eventbus.event.Event;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Florian Anselstetter
 *         Date: 3/15/14
 *         Time: 5:45 PM
 */

public class ThreadEnforcerTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private class TestEvent extends Event {

    }

    private final EventBus.EventCallback<TestEvent> callback = new EventBus.EventCallback<TestEvent>() {
        @Override
        public void onNotify(TestEvent event) {

        }
    };

    private final EventBus bus = new EventBus.Builder()
            .setThreadEnforcer(ThreadEnforcer.SAME)
            .build();

    @Test
    public void testRegisterOnDifferentThreadShouldFail() {
        bus.reset();

        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run() {
                bus.register(TestEvent.class, callback);
                exception.expect(IllegalStateException.class);
            }
        });
        newThread.start();
    }

    @Test
    public void testRegisterOnSameThreadShouldPass() {
        bus.reset();
        bus.register(TestEvent.class, callback);
    }

    @Test
    public void testUnregisterOnDifferentThreadShouldFail() {
        bus.reset();
        bus.register(TestEvent.class, callback);

        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run() {
                bus.unregister(TestEvent.class, callback);
                exception.expect(IllegalStateException.class);
            }
        });
        newThread.start();
    }

    @Test
    public void testUnregisterOnSameThreadShouldPass() {
        bus.reset();
        bus.register(TestEvent.class, callback);
        bus.unregister(TestEvent.class, callback);
    }

    @Test
    public void testPostOnDifferentThreadShouldFail() {
        bus.reset();
        bus.register(TestEvent.class, callback);

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
        bus.reset();
        bus.register(TestEvent.class, callback);
        bus.post(new TestEvent());
    }
}
