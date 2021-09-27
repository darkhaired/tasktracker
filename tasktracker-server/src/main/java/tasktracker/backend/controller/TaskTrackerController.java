package tasktracker.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import tasktracker.backend.ApplicationConfiguration;
import tasktracker.backend.controller.model.*;
import tasktracker.backend.model.*;
import tasktracker.backend.service.TaskTrackerService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static tasktracker.backend.controller.DateTimePatterns.getDateFromFormattedDateString;
import static tasktracker.backend.controller.exception.ApiException.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1")
public class TaskTrackerController {
    private final TaskTrackerService taskTrackerService;
    private final ApplicationConfiguration backendConfiguration;

    private static Date toDate(final String formatted, final Date defaultDate) {
        if (Objects.isNull(formatted) || formatted.isEmpty()) {
            return defaultDate;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(formatted);
        } catch (ParseException e) {
            throw new InvalidInputData(formatted);
        }
    }

    @GetMapping(path = "")
    @ResponseBody
    public Response<String> index() {
        return Response.success("ok");
    }

    @GetMapping(path = "/projects")
    @ResponseBody
    public Response<List<ProjectModel>> getProjects(
            @RequestParam(value = "name", required = false) final String name
    ) {
        if (!Objects.isNull(name) && !name.trim().isEmpty()) {
            return Response.success(
                    taskTrackerService
                            .findProjectByName(name)
                            .map(ProjectModel::new)
                            .map(Collections::singletonList)
                            .orElse(Collections.emptyList()));
        }

        return Response.success(taskTrackerService.findProjects().stream().map(ProjectModel::new).collect(Collectors.toList()));
    }

    @PostMapping(path = "/projects")
    @ResponseBody
    public Response<ProjectModel> saveProject(
            @RequestBody final ProjectModel model
    ) {
        if (taskTrackerService.findProjectById(model.getId()).isPresent()) {
            throw new ProjectAlreadyExists(model.getId());
        }

        if (taskTrackerService.findProjectByName(model.getName()).isPresent()) {
            throw new ProjectAlreadyExists(model.getName());
        }

        Project project = taskTrackerService.save(model.toProject());
        taskTrackerService.getOrCreateSettings(project);

        return Response.success(new ProjectModel(project));
    }

    @GetMapping(path = "/projects/{project_id}/tasks/types")
    @ResponseBody
    public Response<List<TaskTypeModel>> getProjectTaskTypes(
            @PathVariable(name = "project_id") final Long projectId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));

        return Response.success(taskTrackerService.getProjectTaskTypes(project)
                .stream()
                .map(TaskTypeModel::new)
                .collect(Collectors.toList()));
    }

    @PostMapping(path = "/projects/{project_id}/tasks/types")
    @ResponseBody
    public Response<TaskTypeModel> saveProjectTaskType(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestBody final TaskTypeModel model
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        if (Objects.isNull(model.getName()) || model.getName().trim().isEmpty()) {
            throw new InvalidInputData("name");
        }

        final TaskType taskType = model.toTaskType();
        taskType.setProject(project);

        return Response.success(new TaskTypeModel(taskTrackerService.save(taskType)));
    }

    @GetMapping(path = "/projects/{project_id}")
    @ResponseBody
    public Response<ProjectModel> getProjectSummary(
            @PathVariable(name = "project_id") final Long projectId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));

        return Response.success(new ProjectModel(project));
    }

    @DeleteMapping(path = "/projects/{project_id}")
    @ResponseBody
    public Response<Boolean> deleteProject(
            @PathVariable(name = "project_id") final Long projectId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        if (backendConfiguration.isProduction()) {
            return Response.fail(new Response.Error(666, "project deletion prohibited", ""));
        }

        taskTrackerService.delete(project);

        return Response.success(true);
    }

    @GetMapping(path = "/projects/{project_id}/tasksNames")
    @ResponseBody
    public Response<List<String>> getTaskStatesNames(
            @PathVariable(name = "project_id") final Long projectId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final List<String> tasks = taskTrackerService.findAllTaskNames(project.getId())
                .stream()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        return Response.success(tasks);
    }

    @GetMapping(path = "/projects/{project_id}/tasks")
    @ResponseBody
    public Response<List<TaskModel>> getTaskStates(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "name", required = false) final String name,
            @RequestParam(name = "status", required = false) final String status,
            @RequestParam(name = "startDate", required = false) final String formattedStartDate,
            @RequestParam(name = "nominalDate", required = false) final String formattedNominalDate,
            @RequestParam(name = "endDate", required = false) final String formattedEndDate,
            @RequestParam(name = "orderBy", required = false) final String orderBy,
            @RequestParam(name = "limit", required = false) final Integer limit
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));

        final Date startDate = Objects.isNull(formattedStartDate) ? null : getDateFromFormattedDateString(formattedStartDate);
        final Date nominalDate = Objects.isNull(formattedNominalDate) ? null : getDateFromFormattedDateString(formattedNominalDate);
        final Date endDate = Objects.isNull(formattedEndDate) ? null : getDateFromFormattedDateString(formattedEndDate);

        return Response.success(taskTrackerService
                .filterTasks(projectId, name, status, startDate, nominalDate, endDate, orderBy, limit)
                .stream()
                .map(TaskModel::new)
                .collect(Collectors.toList()));
    }

    /**
     * @param projectId - project id, which tasks we want to get
     * @param from      - date in format "YYYY-MM-DD", representing beginning of period?
     * @param to        - date in format "YYYY-MM-DD", representing end of period
     * @param dateType  - value, which can take 3 values - "nominal_date", "start_date", "end_date"
     *                  and defines the grouping order of tasks by dates
     * @param lastOnly  - indicates, should we keep all tasks or only those with latest start date
     * @return
     */
    @GetMapping(path = "/projects/{project_id}/tasksInRange")
    @ResponseBody
    public Response<DailyTasksModel> getTaskStates(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "from") final String from,
            @RequestParam(name = "to") final String to,
            @RequestParam(name = "date_type") final Task.DateType dateType,
            @RequestParam(name = "last_only", required = false, defaultValue = "false") final Boolean lastOnly
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));

        final Date dateFrom = Objects.isNull(from) ? null : getDateFromFormattedDateString(from);
        final Date dateTo = Objects.isNull(to) ? null : getDateFromFormattedDateString(to);

        final DailyTasksModel model = taskTrackerService
                .getTasksInRange(project.getId(), dateFrom, dateTo, dateType, lastOnly);

        return Response.success(model);
    }

    @GetMapping(path = "/projects/{project_id}/tasks/{task_id}")
    @ResponseBody
    public Response<TaskModel> getTaskState(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Task task = taskTrackerService.findTaskById(taskId).orElseThrow(() -> new TaskStateNotFound(taskId));

        return Response.success(new TaskModel(task));
    }

    @PostMapping(path = "/projects/{project_id}/tasks")
    @ResponseBody
    public Response<TaskModel> saveTask(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestBody final TaskModel model
    ) {
        log.info("TaskModel: {}", model);
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));

        final Task task = model.toTaskState();

        final Task result = taskTrackerService.save(project, task);

        return Response.success(new TaskModel(result));
    }

    @GetMapping(path = "/projects/{project_id}/tasks/metrics")
    @ResponseBody
    public Response<List<TaskMetricModel>> getMetrics(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "names", required = false) final String names,
            @RequestParam(name = "startDate", required = false) final String startDate,
            @RequestParam(name = "endDate", required = false) final String endDate
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Date start = toDate(startDate, toDate("1960-01-01", null));
        final Date end = toDate(endDate, new Date());
        final Set<String> metricNames = Objects.isNull(names) || names.isEmpty() ? Collections.emptySet() : Arrays.stream(names.split(",")).collect(Collectors.toSet());

        return Response.success(taskTrackerService.getProjectTasksMetrics(project, metricNames, start, end).stream()
                .map(TaskMetricModel::new).collect(Collectors.toList()));

    }

    @PostMapping(path = "/projects/{project_id}/tasks/{task_id}")
    @ResponseBody
    public Response<TaskModel> updateTaskState(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId,
            @RequestBody final TaskModel model
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Task task = taskTrackerService.findTaskById(taskId).orElseThrow(() -> new TaskStateNotFound(taskId));

        final Task newTaskState = model.toTaskState();

        task.setEndDate(newTaskState.getEndDate());
        task.setState(newTaskState.getState());
        task.setTimestamp(newTaskState.getTimestamp());

        return Response.success(new TaskModel(taskTrackerService.update(project, task)));
    }

    @PostMapping(path = "/projects/{project_id}/tasks/{task_id}/metrics")
    public Response<TaskMetricModel> saveTaskMetric(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId,
            @RequestBody final TaskMetricModel model
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Task task = taskTrackerService.findTaskById(taskId).orElseThrow(() -> new TaskStateNotFound(taskId));

        final TaskMetric metric = model.toTaskMetric();

        return Response.success(new TaskMetricModel(taskTrackerService.save(task, metric)));
    }

    @GetMapping(path = "/projects/{project_id}/tasks/{task_id}/metrics")
    public Response<List<TaskMetricModel>> getTaskMetrics(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Task task = taskTrackerService.findTaskById(taskId).orElseThrow(() -> new TaskStateNotFound(taskId));

        return Response.success(taskTrackerService
                .findMetrics(task)
                .stream()
                .map(TaskMetricModel::new)
                .collect(Collectors.toList()));
    }

    @GetMapping(path = "/projects/{project_id}/tasks/{task_id}/metrics/{metric_id}")
    public Response<TaskMetricModel> getTaskMetric(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId,
            @PathVariable(name = "metric_id") final Long metricId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Task task = taskTrackerService.findTaskById(taskId).orElseThrow(() -> new TaskStateNotFound(taskId));

        return Response.success(taskTrackerService
                .findMetrics(task)
                .stream()
                .filter(metric -> Objects.equals(metric.getId(), metricId))
                .map(TaskMetricModel::new)
                .findFirst()
                .orElseThrow(() -> new TaskMetricNotFound(metricId)));
    }

    @GetMapping(path = "/projects/{project_id}/tasksMetrics")
    public Response<Map<String, List<String>>> getTaskMetricNames(
            @PathVariable(name = "project_id") final Long projectId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Map<String, List<String>> metricsNames = taskTrackerService.getAllTasksMetrics(project.getId());

        return Response.success(metricsNames);
    }

    @PostMapping(path = "/projects/{project_id}/tasks/{task_id}/errors")
    public Response<TaskErrorModel> saveTaskError(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId,
            @RequestBody final TaskErrorModel model
    ) {

        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Task task = taskTrackerService.findTaskById(taskId).orElseThrow(() -> new TaskStateNotFound(taskId));

        final TaskError taskError = model.toTaskError();

        return Response.success(new TaskErrorModel(taskTrackerService.save(task, taskError)));
    }

    @GetMapping(path = "/projects/{project_id}/tasks/{task_id}/errors/{error_id}")
    public Response<TaskErrorModel> getTaskError(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId,
            @PathVariable(name = "error_id") final Long errorId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Task task = taskTrackerService.findTaskById(taskId).orElseThrow(() -> new TaskStateNotFound(taskId));

        return Response.success(taskTrackerService
                .findErrors(task)
                .stream()
                .filter(error -> Objects.equals(error.getId(), errorId))
                .map(TaskErrorModel::new)
                .findFirst()
                .orElseThrow(() -> new TaskErrorNotFound(errorId)));
    }

    @GetMapping(path = "/projects/{project_id}/tasks/{task_id}/errors")
    public Response<List<TaskErrorModel>> getTaskErrors(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Task task = taskTrackerService.findTaskById(taskId).orElseThrow(() -> new TaskStateNotFound(taskId));

        return Response.success(taskTrackerService
                .findErrors(task)
                .stream()
                .map(TaskErrorModel::new)
                .collect(Collectors.toList()));
    }

    @PostMapping(path = "/projects/{project_id}/tasks/{task_id}/warnings")
    public Response<WarningModel> saveWarning(
            @PathVariable(name = "project_id") final Long projectId,
            @PathVariable(name = "task_id") final Long taskId,
            @RequestBody final WarningModel model
    ) {
        final Project project = taskTrackerService.findProjectById(projectId).orElseThrow(() -> new ProjectNotFound(projectId));
        final Task task = taskTrackerService.findTaskById(taskId).orElseThrow(() -> new TaskStateNotFound(taskId));

        final Warning warning = model.toWarning();

        return Response.success(new WarningModel(taskTrackerService.save(task, warning)));
    }

}
