package tasktracker.backend.repository;

import org.springframework.data.repository.CrudRepository;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.TaskError;

import java.util.List;

public interface TaskErrorRepository extends CrudRepository<TaskError, Long> {

    List<TaskError> findByTask(Task task);

}
