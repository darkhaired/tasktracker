package tasktracker.backend.eventbus;

@FunctionalInterface
public interface TaskTrackerEventConsumer {
    
    void onEvent(TaskTrackerEvent message);
    
}
