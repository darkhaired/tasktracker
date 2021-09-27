package tasktracker.backend.service;

import tasktracker.backend.model.Incident;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface IncidentService {

    Incident save(Incident incident);

    List<Incident> findIncidents(Date date);

    Incident create(final Task task);

    List<Incident> findIncidents(Project project);

    List<Incident> findIncidents(tasktracker.backend.model.Project project, Date date);

    List<Incident> findIncidents(tasktracker.backend.model.Project project, Date start, Date end);

    List<Incident> findAll();

    void delete(Incident incident);

    Optional<Incident> findIncidentForTask(Task task);
}
