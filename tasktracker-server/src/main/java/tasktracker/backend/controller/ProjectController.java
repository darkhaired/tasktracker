package tasktracker.backend.controller;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tasktracker.backend.controller.exception.ApiException;
import tasktracker.backend.controller.model.ProjectStatsModel;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;
import tasktracker.backend.service.TaskTrackerService;

import java.util.*;
import java.util.stream.Collectors;

import static tasktracker.backend.controller.ErrorResponse.ErrorResponseBuilder;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v2")
public class ProjectController {
    private final TaskTrackerService taskTrackerService;

    @GetMapping("/projects/{project_id}/stats")
    public ResponseEntity<?> getProjectStats(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "startDate") final String start,
            @RequestParam(name = "endDate") final String end
    ) {
        final ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ApiException.ProjectNotFound(projectId));
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Date startDate = DateTimePatterns.getDateFromFormattedDateStringOrNull(start);
        if (Objects.isNull(startDate)) {
            builder.invalidDateFormat(start);
        }

        final Date endDate = DateTimePatterns.getDateFromFormattedDateStringOrNull(end);
        if (Objects.isNull(endDate)) {
            builder.invalidDateFormat(end);
        }

        if (builder.hasError()) {
            return new ResponseEntity<>(builder.build(), HttpStatus.BAD_REQUEST);
        }

        final List<Task> tasks = taskTrackerService.getProjectTasksByRange(project, startDate, endDate);
        final ProjectStatsModel result = new ProjectStatsModel();
        final Map<String, ProjectStatsModel.DailyProjectStatsModel> date2dailyStats = Maps.newHashMap();
        final Map<String, Set<String>> date2DistinctTasks = Maps.newHashMap();

        for (final Task task : tasks) {
            final String date = DateTimePatterns.toYyyyMmDd(DateUtils.truncate(task.getStartDate(), Calendar.DAY_OF_MONTH));
            final Set<String> distinctTasks = date2DistinctTasks.computeIfAbsent(date, key -> Sets.newHashSet());
            distinctTasks.add(task.getName().toLowerCase().trim());

            final ProjectStatsModel.DailyProjectStatsModel stats =
                    date2dailyStats.computeIfAbsent(date, key -> new ProjectStatsModel.DailyProjectStatsModel());
            stats.setDate(date);
            stats.setTotalTasks(stats.getTotalTasks() + 1);
            stats.setTotalDistinctTasks(distinctTasks.size());

            if (task.isFailed()) {
                stats.setTotalFailedTasks(stats.getTotalFailedTasks() + 1);
            }

            if (task.isRunning()) {
                stats.setTotalRunningTasks(stats.getTotalRunningTasks() + 1);
            }

            if (task.isCompleted()) {
                stats.setTotalCompletedTasks(stats.getTotalCompletedTasks() + 1);
            }
        }

        result.setDates(
                date2dailyStats
                        .values()
                        .stream()
                        .sorted(Comparator.comparing(ProjectStatsModel.DailyProjectStatsModel::getDate))
                        .collect(Collectors.toList())
        );

        return ResponseEntity.ok(result);
    }
}
