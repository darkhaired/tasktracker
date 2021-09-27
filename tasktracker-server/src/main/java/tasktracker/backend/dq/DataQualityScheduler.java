package tasktracker.backend.dq;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.TaskStats;
import tasktracker.backend.service.TaskTrackerService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class DataQualityScheduler {
    private final long ANALYZE_TASKS_PERIOD_IN_MINUTES = 1;

    private final TaskTrackerService taskTrackerService;
    private final DataQualityChecker dataQualityChecker;

    @Scheduled(fixedRate = 1000 * 60 * ANALYZE_TASKS_PERIOD_IN_MINUTES)
    public void analyze() {
        log.info("Tasks analyze process started");

        final List<Project> projects = taskTrackerService.findProjects()
                .stream()
                .filter(project -> project.getSettings()
                        .getTaskDataQualityAllowed())
                .collect(Collectors.toList());

        if (projects.isEmpty()) {
            log.info("No projects allow analyzing data quality");
        }

        for (final Project project : projects) {

            final List<Task> unanalyzedTasks = taskTrackerService.findUnanalyzedTasks(project);

            if (unanalyzedTasks.isEmpty()) {
                log.info("{} project has no tasks to analyze", project);
                continue;
            }

            log.info("{} has total non-analyzed tasks {}", project, unanalyzedTasks.size());

            final long analyzedTasksCount = unanalyzedTasks
                    .stream()
                    .map(task -> {
                        final List<TaskStats> statistics = taskTrackerService.findTaskStats(task);
                        return analyze(project, task, statistics);
                    })
                    .filter(Boolean::booleanValue)
                    .count();

            log.info("{} tasks were analyzed", analyzedTasksCount);
        }

        log.info(
                "Tasks analyze process completed. Next start in {} minutes",
                ANALYZE_TASKS_PERIOD_IN_MINUTES
        );
    }

    public boolean analyze(final Project project, final Task task, final List<TaskStats> statistics) {
//        There is no need to check quality of FAILED tasks
        if (!task.getState().equals(Task.State.FAILED)) {
            dataQualityChecker.applyBasicChecks(task, statistics);
            dataQualityChecker.applyDataQualityChecks(project, task, statistics);
        }

        task.setAnalyzed(true);

        taskTrackerService.update(project, task);

        return true;
    }

}
