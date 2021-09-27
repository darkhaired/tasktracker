package tasktracker.backend.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.TaskMetric;

import java.util.List;

@Repository
public interface TaskMetricRepository extends CrudRepository<TaskMetric, Long> {

    List<TaskMetric> findByTask(Task task);
}
