package tasktracker.backend.controller;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tasktracker.backend.controller.groupers.Frequency;
import tasktracker.backend.controller.model.dashboard.DistributionChart;
import tasktracker.backend.controller.model.dashboard.KeyValueModel;
import tasktracker.backend.controller.model.dashboard.StatisticsChart;
import tasktracker.backend.controller.model.dashboard.TopChart;
import tasktracker.backend.model.DataQualityCondition;
import tasktracker.backend.model.Project;
import tasktracker.backend.service.TaskTrackerService;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static tasktracker.backend.controller.DateTimePatterns.getDateFromFormattedDateStringOrNull;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2")
public class ProjectDashboardController {
    private final TaskTrackerService taskTrackerService;

    @GetMapping(path = "/projects/{project_id}/dashboard/topFailedTasks")
    public ResponseEntity<?> topFailedTasks(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "startDate") final String from,
            @RequestParam(name = "endDate") final String to,
            @RequestParam(name = "top") final int top
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Date startDate = getDateFromFormattedDateStringOrNull(from);
        if (Objects.isNull(startDate)) {
            builder.invalidDateFormat(from);
        }
        final Date endDate = getDateFromFormattedDateStringOrNull(to);
        if (Objects.isNull(endDate)) {
            builder.invalidDateFormat(to);
        }

        if (top < 0) {
            return new ResponseEntity<>(builder.badParam("Top parameter should be > 0").build(), HttpStatus.BAD_REQUEST);
        }
        if (builder.hasError()) {
            return new ResponseEntity<>(builder.build(), HttpStatus.BAD_REQUEST);
        }

        LinkedHashMap<String, Integer> topTasks = taskTrackerService.findTopFailedTasksStats(project, startDate, endDate, top);

        TopChart topChart = new TopChart();
        topChart.setStartDate(from);
        topChart.setEndDate(to);
        topChart.setTop(top);
        topChart.setTopElements(topTasks.entrySet()
                .stream()
                .map(entry -> new TopChart.TopElement(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList())
        );

        return ResponseEntity.ok(topChart);
    }

    @GetMapping(path = "/projects/{project_id}/dashboard/topLongTasks")
    public ResponseEntity<?> topLongTasks(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "startDate") final String from,
            @RequestParam(name = "endDate") final String to,
            @RequestParam(name = "top") final int top
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Date startDate = getDateFromFormattedDateStringOrNull(from);
        if (Objects.isNull(startDate)) {
            builder.invalidDateFormat(from);
        }
        final Date endDate = getDateFromFormattedDateStringOrNull(to);
        if (Objects.isNull(endDate)) {
            builder.invalidDateFormat(to);
        }

        if (builder.hasError()) {
            return new ResponseEntity<>(builder.build(), HttpStatus.BAD_REQUEST);
        }
        if (top < 0) {
            return new ResponseEntity<>(builder.badParam("Top parameter should be > 0").build(), HttpStatus.BAD_REQUEST);
        }

        LinkedHashMap<String, Integer> topTasks = taskTrackerService.findTopLongTasksStats(project, startDate, endDate, top);

        TopChart topChart = new TopChart();
        topChart.setStartDate(from);
        topChart.setEndDate(to);
        topChart.setTop(top);
        topChart.setTopElements(topTasks.entrySet()
                .stream()
                .map(entry -> new TopChart.TopElement(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList())
        );

        return ResponseEntity.ok(topChart);
    }

    @GetMapping(path = "/projects/{project_id}/dashboard/tasksDistribution")
    public ResponseEntity<?> tasksDistribution(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "startDate") final String from,
            @RequestParam(name = "endDate") final String to,
            @RequestParam(name = "timePeriod") final Frequency frequency
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Date startDate = getDateFromFormattedDateStringOrNull(from);
        if (Objects.isNull(startDate)) {
            builder.invalidDateFormat(from);
        }
        final Date endDate = getDateFromFormattedDateStringOrNull(to);
        if (Objects.isNull(endDate)) {
            builder.invalidDateFormat(to);
        }

        if (builder.hasError()) {
            return new ResponseEntity<>(builder.build(), HttpStatus.BAD_REQUEST);
        }

        Map<Date, Integer> result = taskTrackerService.getTasksDistribution(project, startDate, endDate, frequency);

        SimpleDateFormat df = DateTimePatterns.getDateTimeFormat();

        DistributionChart chart = new DistributionChart();
        chart.setStartDate(from);
        chart.setEndDate(to);
        chart.setTimePeriod(frequency.name());
        chart.setData(result.entrySet()
                .stream()
                .map(entry -> new DistributionChart.Distribution(df.format(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList()));

        return ResponseEntity.ok(chart);
    }

    @GetMapping(path = "/projects/{project_id}/dashboard/topErrors")
    public ResponseEntity<?> topErrors(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "startDate") final String from,
            @RequestParam(name = "endDate") final String to,
            @RequestParam(name = "top") final int top
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Date startDate = getDateFromFormattedDateStringOrNull(from);
        if (Objects.isNull(startDate)) {
            builder.invalidDateFormat(from);
        }
        final Date endDate = getDateFromFormattedDateStringOrNull(to);
        if (Objects.isNull(endDate)) {
            builder.invalidDateFormat(to);
        }

        if (builder.hasError()) {
            return new ResponseEntity<>(builder.build(), HttpStatus.BAD_REQUEST);
        }
        if (top < 0) {
            return new ResponseEntity<>(builder.badParam("Top parameter should be > 0").build(), HttpStatus.BAD_REQUEST);
        }

        List<TopChart.TopElement> topTasks = taskTrackerService.findTopErrors(project, startDate, endDate, top);
        TopChart topChart = new TopChart();
        topChart.setStartDate(from);
        topChart.setEndDate(to);
        topChart.setTop(top);
        topChart.setTopElements(topTasks);

        return ResponseEntity.ok(topChart);
    }


    @GetMapping(path = "/projects/{project_id}/dashboard/topWarnings")
    public ResponseEntity<?> topWarnings(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "startDate") final String from,
            @RequestParam(name = "endDate") final String to,
            @RequestParam(name = "top") final int top
    ) {
        final ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder();

        final Project project = taskTrackerService.findProjectById(projectId).orElse(null);
        if (Objects.isNull(project)) {
            return new ResponseEntity<>(builder.projectNotFound(projectId).build(), HttpStatus.NOT_FOUND);
        }

        final Date startDate = getDateFromFormattedDateStringOrNull(from);
        if (Objects.isNull(startDate)) {
            builder.invalidDateFormat(from);
        }
        final Date endDate = getDateFromFormattedDateStringOrNull(to);
        if (Objects.isNull(endDate)) {
            builder.invalidDateFormat(to);
        }

        if (builder.hasError()) {
            return new ResponseEntity<>(builder.build(), HttpStatus.BAD_REQUEST);
        }
        if (top < 0) {
            return new ResponseEntity<>(builder.badParam("Top parameter should be > 0").build(), HttpStatus.BAD_REQUEST);
        }

        List<TopChart.TopWarningElement> topTasks = taskTrackerService.findTopWarnings(project, startDate, endDate, top);
        TopChart topChart = new TopChart();
        topChart.setStartDate(from);
        topChart.setEndDate(to);
        topChart.setTop(top);
        topChart.setTopElements(topTasks);

        return ResponseEntity.ok(topChart);
    }

    @GetMapping(path = "/projects/{project_id}/dashboard/statistics")
    public ResponseEntity<?> getTaskStatsValues(
            @PathVariable(name = "project_id") final Long projectId,
            @RequestParam(name = "taskName") final String taskName,
            @RequestParam(name = "startDate") final String from,
            @RequestParam(name = "endDate") final String to,
            @RequestParam(name = "table") final String table,
            @RequestParam(name = "column") final String column,
            @RequestParam(name = "metric") final DataQualityCondition.Metric metric
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
        if (Strings.isNullOrEmpty(column)) {
            return new ResponseEntity<>(builder.badParam("column").build(), HttpStatus.NOT_FOUND);
        }
        if (Strings.isNullOrEmpty(table)) {
            return new ResponseEntity<>(builder.badParam("table").build(), HttpStatus.NOT_FOUND);
        }

        final List<String> taskNames = taskTrackerService.findAllTaskNames(project.getId())
                .stream()
                .map(name -> TaskTrackerService.normalizeTaskName(name))
                .collect(Collectors.toList());
        if (!taskNames.contains(TaskTrackerService.normalizeTaskName(taskName))) {
            return new ResponseEntity<>(builder.addError(String.format("There is no such task '%s'", taskName)).build(), HttpStatus.NOT_FOUND);
        }

        final List<KeyValueModel> keyValueModels = taskTrackerService.getTaskStats(project, taskName, startDate, endDate, table, column, metric);
        if (keyValueModels.isEmpty()) {
            return new ResponseEntity<>(builder.addError(String.format("There were no finished tasks for this period or '%s' metric has only null values", metric)).build(), HttpStatus.OK);
        }

        StatisticsChart chart = new StatisticsChart();
        chart.setStartDate(from);
        chart.setEndDate(to);
        chart.setValues(keyValueModels);
        return ResponseEntity.ok(chart);
    }
}
