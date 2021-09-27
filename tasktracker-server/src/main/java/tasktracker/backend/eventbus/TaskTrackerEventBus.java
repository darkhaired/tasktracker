package tasktracker.backend.eventbus;

public interface TaskTrackerEventBus {

    void post(TaskTrackerEvent event);

    void register(TaskTrackerEventConsumer consumer);

    void unregister(TaskTrackerEventConsumer consumer);

}
