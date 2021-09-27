package tasktracker.backend.service;

import com.google.common.collect.Maps;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class StatisticsBasedTaskStateUpdater implements TaskStateUpdater {
    private final Logger logger = LoggerFactory.getLogger(getClass());


    @Transactional // TODO: требуется декомпозиция вычислений, так как транзакция может выполнятся достаточно долго
    @Override
    public List<Task> update(final TaskTrackerService service) {
        final List<Project> projects = service.findProjects();
        logger.info("All projects: '{}'", projects);

        projects.forEach(service::getOrCreateSettings);

        final List<Project> filteredProjects = projects
                .stream()
                .filter(project -> project.getSettings().getAutoStatusUpdate())
                .collect(Collectors.toList());

        logger.info("Projects with auto-update of task statuses: '{}'", filteredProjects);

        return filteredProjects.stream().flatMap(project -> {
            logger.info("Update tasks stats for project '{}'", project);

            final List<Task> tasks = project.getTaskStates();
            final List<Task> tasksWithDefinedExecutionTime = tasks
                    .stream()
                    .filter(task -> Objects.nonNull(task.getStartDate()) && Objects.nonNull(task.getEndDate()))
                    .collect(Collectors.toList());

            final Map<String, List<Task>> taskName2tasksWithDefinedExecutionTime = tasksWithDefinedExecutionTime
                    .stream()
                    .collect(Collectors.groupingBy(Task::getName));

            final Map<String, DescriptiveStatistics> taskName2stats = Maps.newHashMap();

            taskName2tasksWithDefinedExecutionTime.forEach((key, value) ->
                    value.forEach(task -> taskName2stats.computeIfAbsent(key, k -> new DescriptiveStatistics()).addValue(service.getExecutionTime(task))));

            final List<Task> taskWithUndefinedExecutionTime = tasks
                    .stream()
                    .filter(task -> Objects.isNull(task.getEndDate()) && task.getState().equals(Task.State.RUNNING))
                    .collect(Collectors.toList());

            logger.info("Project tasks - total: {}, with end time: {}, without end time: {}",
                    tasks.size(),
                    tasksWithDefinedExecutionTime.size(),
                    taskWithUndefinedExecutionTime.size());

            return taskWithUndefinedExecutionTime
                    .stream()
                    .filter(task -> {
                        final DescriptiveStatistics stats = taskName2stats.get(task.getName());
                        if (Objects.isNull(stats)) {
                            return false;
                        }

                        final double stdDev = stats.getStandardDeviation();
                        final double upperBound = stats.getPercentile(50) + 3 * stdDev;

                        logger.info("Task name '{}', std dev: '{}', upper bound of execution time '{}'", task.getName(), stdDev, upperBound);
                        final long executionTime = service.getExecutionTime(task, new Date());
                        logger.info("Task '{}': execution time '{}'", task, executionTime);

                        return upperBound > 0 && executionTime >= upperBound;
                    })
                    .peek(task -> {
                        final Task.State state = Task.State.FAILED;
                        final Date endTime = new Date();

                        task.setState(state);
                        task.setEndDate(endTime);
                        task.setAutoUpdated(true);

                        logger.info("Update task: state '{}', end time '{}'", state, endTime);

                        service.update(project, task);
                    });

        }).collect(Collectors.toList());
    }

}
