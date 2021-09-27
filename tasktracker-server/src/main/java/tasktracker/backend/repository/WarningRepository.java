package tasktracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.Warning;

import java.util.Date;
import java.util.List;

@Repository
public interface WarningRepository extends JpaRepository<Warning, Long> {

    @Query("SELECT w FROM Warning w WHERE w.task = :task")
    List<Warning> findByTask(@Param("task") Task task);

    @Query("from Warning w where w.task.project = :project and " +
            "cast(w.createdTime as date) >= cast(:start as date) and " +
            "cast(w.createdTime as date) <= cast(:end as date)")
    List<Warning> findWarningsInRange(
            @Param("project") Project project,
            @Param("start") Date start,
            @Param("end") Date end);

}
