package tasktracker.backend.service;

import tasktracker.backend.model.Task;

import java.util.List;

public interface TaskStateUpdater {

    List<Task> update(TaskTrackerService service);

}
