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

public interface ThreadEnforcer {

    /**
     * Allow any thread.
     */
    public ThreadEnforcer ANY = new ThreadEnforcer() {
        @Override
        public void enforce(EventBus eventBus) {

        }
    };

    /**
     * Allow operations only on the same thread.
     */
    public ThreadEnforcer SAME = new ThreadEnforcer() {
        @Override
        public void enforce(EventBus eventBus) {
            if (Thread.currentThread() != eventBus.getRunningThread()) {
                throw new IllegalStateException(
                        "Event bus " + eventBus + " accessed from another thread " + Thread.currentThread());
            }
        }
    };

    /**
     * Enforce calling thread policy
     *
     * @param eventBus The used event bus
     */
    public void enforce(EventBus eventBus);
}
