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

package net.anselstetter.eventbus.transport;

import net.anselstetter.eventbus.EventBus;
import net.anselstetter.eventbus.event.Event;

/**
 * @author Florian Anselstetter
 *         Date: 3/15/14
 *         Time: 5:38 PM
 */
public interface EventTransport {

    /**
     * Delivers the event to its listener
     *
     * @param event    The posted Event
     * @param callback The callback to invoke
     */
    public void deliver(Event event, EventBus.EventCallback<Event> callback);
}
