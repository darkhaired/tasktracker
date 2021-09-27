package tasktracker.backend.eventbus;


import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DefaultEventBus implements TaskTrackerEventBus {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CopyOnWriteArraySet<TaskTrackerEventConsumer> consumers = new CopyOnWriteArraySet<>();
    private final Executor executor;

    public DefaultEventBus() {
        this.executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactoryBuilder().setNameFormat("tt-event-bus-pool-%d").build()
        );
    }

    @Override
    public void post(final TaskTrackerEvent event) {
        Preconditions.checkNotNull(event);
        dispatch(event);
    }

    @Override
    public void register(final TaskTrackerEventConsumer consumer) {
        Preconditions.checkNotNull(consumer);
        logger.info("Register consumer {}", consumer);
        consumers.add(consumer);
    }

    @Override
    public void unregister(final TaskTrackerEventConsumer consumer) {
        Preconditions.checkNotNull(consumer);
        logger.info("Unregister consumer {}", consumer);
        consumers.remove(consumer);
    }

    private void dispatch(final TaskTrackerEvent event) {
        executor.execute(() -> consumers.forEach(consumer -> consumer.onEvent(event)));
    }
}
