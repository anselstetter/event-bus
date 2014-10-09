Yet another event bus.

Usage:

    EventBus bus = new EventBus.Builder()
            .setTransport(EventTransport.STANDARD)
            .setThreadEnforcer(ThreadEnforcer.ANY)
            .setExecutorService(Executors.newFixedThreadPool(10))
            .setIdentifier("identifier")
            .build();

    // Normal subscription
    bus
            .on(TestEvent.class)
            .callback(new EventCallback<TestEvent>() {
                @Override
                public void onEvent(TestEvent event) {
                    // Do something
                }
            });

    // Deliver last posted event on registration
    bus
            .on(TestEvent.class)
            .deliverLastEvent()
            .callback(new EventCallback<TestEvent>() {
                @Override
                public void onEvent(TestEvent event) {
                    // Do something
                }
            });

    // Tag based subscription
    // You can call bus.unregister("TAG") later
    bus
            .on(TestEvent.class)
            .setTag("TAG")
            .callback(new EventCallback<TestEvent>() {
                @Override
                public void onEvent(TestEvent event) {
                    // Do something
                }
            });
    
    // Set the priority for event delivery
    // The highest number means the highest priority
    bus
            .on(TestEvent.class)
            .setPriority(100)
            .callback(new EventCallback<TestEvent>() {
                @Override
                public void onEvent(TestEvent event) {
                    // Do something
                }
            });

    // Deliver an event synchronously
    bus.post(new TestEvent("payload"));
    
    // Deliver an event asynchronously
    bus.async(new TestEvent("payload"));
    
    // Deliver an event asynchronously with a finished notification
    bus.async(new TestEvent("payload"), new EventBus.AsyncNotificationListener() {
        @Override
        public void onFinish() {
            // Do something
        }
    });

    // Unregister an event callback
    bus
            .on(TestEvent.class)
            .unregister(callback);
        
    // Unregister all event callbacks with the same tag
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