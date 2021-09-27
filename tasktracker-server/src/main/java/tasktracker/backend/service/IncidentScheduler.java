package tasktracker.backend.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tasktracker.backend.model.Incident;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.ProjectSettings;
import tasktracker.backend.model.Task;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

@RequiredArgsConstructor
@Component
public class IncidentScheduler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TaskTrackerService taskTrackerService;
    private final IncidentService incidentService;

    // once in 10 minutes
//    @Scheduled(fixedRate = 1000 * 60 * 2)
//    @Scheduled(fixedRate = 5000)
    public void run() {
        logger.info("Failed task processing started");
        final Date date = new Date();
        final List<Project> projects = taskTrackerService.findProjects();

        final ConcurrentMap<Project, List<Incident>> projectIncidents = Maps.newConcurrentMap();

        for (final Project project : projects) {
            final ProjectSettings settings = project.getSettings();
            if (Objects.isNull(settings)) {
                logger.info("No projects settings for project {}", project.getName());
                continue;
            }
            if (Objects.isNull(settings.getIncidentGenerationAllowed()) || !settings.getIncidentGenerationAllowed()) {
                logger.info("For project {} incidents generation not allowed", project.getName());
                continue;
            }

            for (final Task task : taskTrackerService.findTaskByDateAndState(date, Task.State.FAILED)) {
                if (!incidentService.findIncidentForTask(task).isPresent()) {
                    logger.info("Create incident for failed task {}", task.getId());
                    final Incident incident = incidentService.create(task);
                    projectIncidents.computeIfAbsent(project, (p) -> Lists.newArrayList()).add(incident);
                }
            }
        }

        logger.info("Failed task processing completed");
        for (final Project project : projectIncidents.keySet()) {
            logger.info("Summary: Project '{}' - {} new incidents", project.getName(), projectIncidents.get(project).size());
        }
    }

}
