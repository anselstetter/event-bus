Yet another event bus.

Usage:

    EventBus bus = new EventBus.Builder()
            .setTransport(new InMemoryTransport())
            .setThreadEnforcer(ThreadEnforcer.ANY)
            .setIdentifier("identifier")
            .build();

    // Normal subscription
    bus.register(TestEvent.class, new EventBus.EventCallback<TestEvent>() {
        @Override
        public void onNotify(TestEvent event) {
            // Do something
        }
    });

    // Deliver last posted event on registration
    bus.register(TestEvent.class, new EventBus.EventCallback<TestEvent>() {
        @Override
        public void onNotify(TestEvent event) {
            // Do something
        }
    }, true);

    // Tag based subscription
    // You can call bus.unregister("TAG") later
    bus.register(TestEvent.class, new EventBus.EventCallback<TestEvent>() {
        @Override
        public void onNotify(TestEvent event) {
            // Do something
        }
    }, "TAG");

    // Tag based subscription with last event delivery on registration
    bus.register(TestEvent.class, new EventBus.EventCallback<TestEvent>() {
        @Override
        public void onNotify(TestEvent event) {
            // Do something
        }
    }, true, "TAG");

    bus.post(new TestEvent("payload"));

    bus.unregister(TestEvent.class, callback);
    bus.unregister("TAG")

This bus does not guarantee uniqueness.
You can have as many buses as you want.
Drop it in a singleton, if you need uniqueness.

Use it globally like this:

    public class GlobalBus {

        private static EventBus instance = new EventBus.Builder()
                .setTransport(new InMemoryTransport())
                .setThreadEnforcer(ThreadEnforcer.ANY)
                .setIdentifier("identifier")
                .build();

        private GlobalBus() {
    
        }

        public static EventBus get() {
            return instance;
        }
    }

Type "mvn install" and you'll find a jar in the target directory.

It's Apache license. Have fun.