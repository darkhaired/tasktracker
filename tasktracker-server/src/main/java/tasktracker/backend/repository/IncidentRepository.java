package tasktracker.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tasktracker.backend.model.Incident;
import tasktracker.backend.model.Project;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query("select i from Incident i where cast(i.date as date) = cast(?1 as date)")
    List<Incident> findIncidents(Date date);

    @Query("select i from Incident i where i.task.project = ?1")
    List<Incident> findIncidents(Project project);

    @Query("select i from Incident i where i.task.project = ?1 and cast(i.date as date) = cast(?2 as date)")
    List<Incident> findIncidents(Project project, Date date);

    Optional<Incident> findByTaskId(Long id);
}
