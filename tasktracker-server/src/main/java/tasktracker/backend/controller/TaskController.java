package tasktracker.backend.controller;

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tasktracker.backend.controller.body.ChangeTasksNameBody;
import tasktracker.backend.controller.body.TaskStatsCollectionBody;
import tasktracker.backend.controller.exception.ApiException;
import tasktracker.backend.controller.mappers.TaskStatsMapper;
import tasktracker.backend.controller.model.*;
import tasktracker.backend.model.Project;
import tasktracker.backend.model.Task;
import tasktracker.backend.model.TaskStats;
import tasktracker.backend.service.TaskFilter;
import tasktracker.backend.service.TaskTrackerService;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static tasktracker.backend.controller.DateTimePatterns.getDateFromFormattedDateStringOrNull;
import static tasktracker.backend.model.Task.State;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2")
public class TaskController {
    private final TaskTrackerService taskTrackerService;
    private final TaskStatsMapper taskStatsMapper;
    private final WarningMapper warningMapper;

    @PatchMapping(path = "/projects/{project_id}/tasks/rename")
    public ResponseEntity<?> changeTasksNames(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestBody final ChangeTasksNameBody body
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        if (StringUtils.isEmpty(body.getOriginalName())) {
            builder.paramMissing("original_name");
        }
        if (StringUtils.isEmpty(body.getNewName())) {
            builder.paramMissing("new_name");
        }

        if (builder.hasError()) {
            return ResponseEntity.badRequest().body(builder.build());
        }

        taskTrackerService.renameTasks(body.getOriginalName(), body.getNewName());

        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/projects/{project_id}/tasks")
    @ResponseBody
    public ResponseEntity<?> getTasks(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "startDate") final String from,
            @RequestParam(name = "endDate") final String to,
            @RequestParam(name = "names", required = false, defaultValue = "") final Set<String> names,
            @RequestParam(name = "statuses", required = false, defaultValue = "") final Set<String> statuses,
            @RequestParam(name = "dateType") final Task.DateType dateType,
            @RequestParam(name = "limit", required = false, defaultValue = "0") final int limit,
            @RequestParam(name = "fields", required = false) final Set<String> fields
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Date startDate = getDateFromFormattedDateStringOrNull(from);
        final Date endDate = getDateFromFormattedDateStringOrNull(to);
        final Set<String> includeFields = fields == null ?
                Sets.newHashSet() :
                fields.stream().map(field -> field.trim().toLowerCase()).collect(Collectors.toSet());

        TaskFilter filter = TaskFilter
                .of()
                .setNames(names)
                .setStatuses(statuses)
                .setFrom(startDate)
                .setTo(endDate)
                .setDateType(dateType)
                .setLimit(limit)
                .setWithStatistics(includeFields.contains("statistics"))
                .setWithErrors(includeFields.contains("errors"))
                .setWithMetrics(includeFields.contains("metrics"));
        final List<Task> tasks = taskTrackerService.findTasks(project, filter);

        return ResponseEntity.ok(
                tasks
                        .stream()
                        .map(task -> {
                            final TaskModel model = new TaskModel(task);
                            if (filter.isWithMetrics()) {
                                model.setMetrics(taskTrackerService.findMetrics(task).stream().map(TaskMetricModel::new).collect(Collectors.toList()));
                            }
                            if (filter.isWithStatistics()) {
                                model.setStatistics(taskTrackerService.findStatistics(task).stream().map(taskStatsMapper::to).collect(Collectors.toList()));
                            }
                            if (filter.isWithErrors()) {
                                model.setErrors(taskTrackerService.findErrors(task).stream().map(TaskErrorModel::new).collect(Collectors.toList()));
                            }
                            return model;
                        })
                        .collect(Collectors.toList())
        );
    }

    @GetMapping(path = "/projects/{project_id}/tasks/report")
    @ResponseBody
    public ResponseEntity<?> getTasksReport(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "startDate") final String from,
            @RequestParam(name = "endDate") final String to,
            @RequestParam(name = "names", required = false, defaultValue = "") final Set<String> names,
            @RequestParam(name = "statuses", required = false, defaultValue = "") final Set<String> statuses,
            @RequestParam(name = "dateType") final Task.DateType dateType,
            @RequestParam(name = "fields", required = false) final Set<String> fields,
            @RequestParam(name = "lastTaskOnly", required = false, defaultValue = "false") final Boolean lastTaskOnly
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Date startDate = getDateFromFormattedDateStringOrNull(from);
        if (Objects.isNull(startDate)) {
            return new ResponseEntity<>(builder.badParam("startDate").build(), HttpStatus.NOT_FOUND);
        }
        final Date endDate = getDateFromFormattedDateStringOrNull(to);
        if (Objects.isNull(endDate)) {
            return new ResponseEntity<>(builder.badParam("endDate").build(), HttpStatus.NOT_FOUND);
        }

        final Set<String> includeFields = fields == null ?
                Sets.newHashSet() :
                fields.stream().map(field -> field.trim().toLowerCase()).collect(Collectors.toSet());

        final Set<String> taskNames = taskTrackerService.findTaskNamesByFilter(project, names);
        final Set<State> taskStatuses = statuses.stream().map(State::findByName).collect(Collectors.toSet());

        final TaskFilter filter = TaskFilter
                .of()
                .setFrom(startDate)
                .setTo(endDate)
                .setDateType(dateType)
                .setNames(taskNames)
                .setWithStatistics(includeFields.contains("statistics"))
                .setWithErrors(includeFields.contains("errors"))
                .setWithMetrics(includeFields.contains("metrics"))
                .setWithWarnings(includeFields.contains("warnings"));

        List<TasksReportModel> result = taskTrackerService.groupTasksByNameAndDate(project, filter, taskStatuses, lastTaskOnly);

        final TasksReportResponse response = new TasksReportResponse();
        response.setTasks(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/projects/{project_id}/tasks/{task_id}/statistics")
    public ResponseEntity<?> saveTaskStats(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Task task = taskTrackerService.findTaskById(taskId).orElse(null);
        if (Objects.isNull(task)) {
            return new ResponseEntity<>(builder.taskNotFound(taskId).build(), HttpStatus.NOT_FOUND);
        }

        final List<TaskStats> result = taskTrackerService.findTaskStats(task);

        return ResponseEntity.ok(taskStatsMapper.to(result));
    }

    @GetMapping(path = "/projects/{project_id}/tasks/{task_id}/details")
    public ResponseEntity<?> getTaskDetails(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId,
            @RequestParam(name = "fields", required = false) final Set<String> fields
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Task task = taskTrackerService.findTaskById(taskId).orElse(null);
        if (Objects.isNull(task)) {
            return new ResponseEntity<>(builder.taskNotFound(taskId).build(), HttpStatus.NOT_FOUND);
        }

        final Set<String> includeFields = fields == null ?
                Sets.newHashSet() :
                fields.stream().map(field -> field.trim().toLowerCase()).collect(Collectors.toSet());

        TaskModel taskModel = new TaskModel(task);

        if (includeFields.contains("metrics")) {
            taskModel.setMetrics(taskTrackerService.findMetrics(task).stream().map(TaskMetricModel::new).collect(Collectors.toList()));
        }
        if (includeFields.contains("statistics")) {
            taskModel.setStatistics(taskTrackerService.findStatistics(task).stream().map(taskStatsMapper::to).collect(Collectors.toList()));
        }
        if (includeFields.contains("errors")) {
            taskModel.setErrors(taskTrackerService.findErrors(task).stream().map(TaskErrorModel::new).collect(Collectors.toList()));
        }
        if (includeFields.contains("warnings")) {
            taskModel.setWarnings(taskTrackerService.findWarnings(task).stream().map(WarningModel::new).collect(Collectors.toList()));
        }

        return ResponseEntity.ok(taskModel);
    }

    @PostMapping(path = "/projects/{project_id}/tasks/{task_id}/statistics")
    public ResponseEntity<?> saveTaskStatistics(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId,
            @RequestBody final TaskStatsCollectionBody body
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Task task = taskTrackerService.findTaskById(taskId).orElse(null);
        if (Objects.isNull(task)) {
            return new ResponseEntity<>(builder.taskNotFound(taskId).build(), HttpStatus.NOT_FOUND);
        }

        if (Objects.isNull(body.getStats())) {
            return new ResponseEntity<>(builder.badParam("body").build(), HttpStatus.BAD_REQUEST);
        }

        final List<TaskStats> stats = body.getStats().stream().map(taskStatsMapper::to).collect(Collectors.toList());

        final List<TaskStats> result = taskTrackerService.saveTaskStats(task, stats);

        return ResponseEntity.ok(taskStatsMapper.to(result));
    }

    @GetMapping(path = "/projects/{project_id}/tasksNames")
    @ResponseBody
    public ResponseEntity<?> getAllTaskNames(
            @PathVariable(name = "project_id") final Long projectId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ApiException.ProjectNotFound(projectId));
        final List<String> tasks = taskTrackerService.findAllTaskNames(project.getId())
                .stream()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        return ResponseEntity.ok(tasks);
    }
}
