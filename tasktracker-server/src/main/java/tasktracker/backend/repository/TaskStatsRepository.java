package tasktracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.TaskStats;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskStatsRepository extends JpaRepository<TaskStats, Long> {

    @Query("SELECT s FROM TaskStats s WHERE task = :task")
    List<TaskStats> findByTask(@Param("task") Task task);

    Optional<TaskStats> findTopByColumnOrderByIdDesc(String column);
}
