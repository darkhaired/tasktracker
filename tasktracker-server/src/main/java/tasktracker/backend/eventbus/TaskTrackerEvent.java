package tasktracker.backend.eventbus;

public class TaskTrackerEvent {
    private final Object object;

    public TaskTrackerEvent(final Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public static TaskTrackerEvent of(final Object object) {
        return new TaskTrackerEvent(object);
    }
}
