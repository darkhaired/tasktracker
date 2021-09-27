package tasktracker.backend.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import tasktracker.backend.model.TaskType;

import java.util.List;

@Repository
public interface TaskTypeRepository extends CrudRepository<TaskType, Long> {

    List<TaskType> findByProjectId(Long projectId);

}
