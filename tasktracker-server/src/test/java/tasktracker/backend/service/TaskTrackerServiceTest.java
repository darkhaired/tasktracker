package tasktracker.backend.service;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import tasktracker.backend.controller.DateTimePatterns;
import tasktracker.backend.controller.groupers.Frequency;
import tasktracker.backend.controller.model.TableModel;
import tasktracker.backend.controller.model.TasksReportModel;
import tasktracker.backend.controller.model.dashboard.TopChart;
import tasktracker.backend.model.*;
import tasktracker.backend.model.Task.State;

import java.util.*;
import java.util.function.BiFunction;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tasktracker.backend.TestHelper.*;
import static tasktracker.backend.controller.DateTimePatterns.getDateFromFormattedDateString;


public class TaskTrackerServiceTest extends AbstractServiceTest {

    private static Project project;
    @InjectMocks
    private TaskTrackerService taskTrackerService;

    @BeforeClass
    public static void setUp() {
        System.setProperty("user.timezone", "UTC");
        project = new Project();
        project.setId(1L);
        project.setName("test");
    }

    @After
    public void tearDown() {

    }

    @Test
    public void setIsTerminalState() {
        final Task task = new Task();

        task.setState(State.RUNNING);
        Assert.assertFalse(taskTrackerService.isTerminalState(task));

        task.setState(State.SCHEDULED);
        Assert.assertFalse(taskTrackerService.isTerminalState(task));

        // Terminal states
        task.setState(State.FAILED);
        Assert.assertTrue(taskTrackerService.isTerminalState(task));

        task.setState(State.SUCCEEDED);
        Assert.assertTrue(taskTrackerService.isTerminalState(task));

        task.setState(State.CANCELED);
        Assert.assertTrue(taskTrackerService.isTerminalState(task));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setIsTerminalStateWhenStateIsNull() {
        final Task task = new Task();

        taskTrackerService.isTerminalState(task);
    }

    @Test
    public void getTaskGroupingByNameAndDateSucceeded() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task(project, "TestTask1", "FAILED", "2020-01-01T08:00:00.000+0000", "2020-01-05T08:00:00.000+0000"));
        tasks.add(task(project, "TestTask1", "SUCCEEDED", "2020-01-01T08:00:00.000+0000", "2020-01-05T12:00:00.000+0000"));
        tasks.add(task(project, "TestTask2", "SUCCEEDED", "2020-01-02T07:01:00.000+0000", "2020-02-05T11:00:00.000+0000"));
        tasks.add(task(project, "TestTask3", "FAILED", "2020-01-03T015:00:00.000+0000", "2020-01-05T12:00:00.000+0000"));

        when(taskRepository.findTasks(any(), any(), any()))
                .thenReturn(tasks);

        Date startDate = getDateFromFormattedDateString("2020-01-01");
        Date endDate = getDateFromFormattedDateString("2020-01-03");

        TaskFilter filter = TaskFilter
                .of()
                .setNames(Sets.newHashSet("TestTask2", "TestTask1", "TestTask3"))
                .setFrom(startDate)
                .setTo(endDate)
                .setDateType(Task.DateType.NOMINAL_DATE)
                .setWithStatistics(false)
                .setWithErrors(false)
                .setWithMetrics(false)
                .setWithWarnings(false);
        Set<State> statuses = Sets.newHashSet(State.SUCCEEDED);
        List<TasksReportModel> filteredTasks = taskTrackerService.groupTasksByNameAndDate(new Project(), filter, statuses, false);

        assertNotNull(filteredTasks);
        assertThat(filteredTasks, hasSize(2));

        assertThat(filteredTasks, hasItem(hasProperty("name", equalTo("TestTask1"))));
        assertThat(filteredTasks, hasItem(hasProperty("name", equalTo("TestTask2"))));
        assertThat(filteredTasks, not(hasItem(hasProperty("name", equalTo("TestTask3")))));

        filteredTasks.forEach((tasksReportModel -> {
            Map<String, Integer> dateToTasksSize;
            switch (tasksReportModel.getName()) {
                case "TestTask1":
                    dateToTasksSize = ImmutableMap.of("2020-01-01", 2, "2020-01-02", 0, "2020-01-03", 0);
                    assertTaskDatesSizeEquals(tasksReportModel, dateToTasksSize);
                    return;
                case "TestTask2":
                    dateToTasksSize = ImmutableMap.of("2020-01-01", 0, "2020-01-02", 1, "2020-01-03", 0);
                    assertTaskDatesSizeEquals(tasksReportModel, dateToTasksSize);
                    return;
            }
        }));
    }

    @Test
    public void getTaskGroupingByNameAndDateFailed() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task(project, "TestTask1", "SUCCEEDED", "2020-01-01T08:00:00.000+0000", "2020-01-05T12:00:00.000+0000"));
        tasks.add(task(project, "TestTask2", "SUCCEEDED", "2020-01-02T07:01:00.000+0000", "2020-02-05T11:00:00.000+0000"));
        tasks.add(task(project, "TestTask2", "FAILED", "2020-01-02T07:01:00.000+0000", "2020-02-06T11:00:00.000+0000"));
        tasks.add(task(project, "TestTask3", "RUNNING", "2020-01-03T015:00:00.000+0000", "2020-01-05T12:00:00.000+0000"));
        tasks.add(task(project, "TestTask4", "SUCCEEDED", "2020-01-02T07:01:00.000+0000", "2020-02-05T11:00:00.000+0000"));

        when(taskRepository.findTasks(any(), any(), any()))
                .thenReturn(tasks);

        Date startDate = getDateFromFormattedDateString("2020-01-01");
        Date endDate = getDateFromFormattedDateString("2020-01-04");

        TaskFilter filter = TaskFilter
                .of()
                .setNames(Sets.newHashSet())
                .setFrom(startDate)
                .setTo(endDate)
                .setDateType(Task.DateType.NOMINAL_DATE)
                .setWithStatistics(false)
                .setWithErrors(false)
                .setWithMetrics(false)
                .setWithWarnings(false);
        Set<State> statuses = Sets.newHashSet(State.FAILED);
        List<TasksReportModel> filteredTasks = taskTrackerService.groupTasksByNameAndDate(new Project(), filter, statuses, false);

        assertNotNull(filteredTasks);
        assertThat(filteredTasks, hasSize(1));

        assertThat(filteredTasks, hasItem(hasProperty("name", equalTo("TestTask2"))));

        filteredTasks.forEach((tasksReportModel -> {
            Map<String, Integer> dateToTasksSize;
            switch (tasksReportModel.getName()) {
                case "TestTask2":
                    dateToTasksSize = ImmutableMap.of("2020-01-01", 0, "2020-01-02", 2, "2020-01-03", 0);
                    assertTaskDatesSizeEquals(tasksReportModel, dateToTasksSize);
                    return;
            }
        }));
    }

    @Test
    public void getTopFailedTasksStats() {
        List<Task> tasks = new ArrayList<>();

        tasks.add(task(project, "TestTask1", "SUCCEEDED", "2020-01-01", "2020-01-01"));
        tasks.add(task(project, "TestTask1", "FAILED", "2020-01-02", "2020-01-02"));
        tasks.add(task(project, "TestTask1", "FAILED", "2020-01-01", "2020-01-01"));

        tasks.add(task(project, "TestTask2", "FAILED", "2020-01-02", "2020-01-02"));
        tasks.add(task(project, "TestTask2", "FAILED", "2020-01-04", "2020-01-04"));
        tasks.add(task(project, "TestTask2", "FAILED", "2020-01-05", "2020-01-05"));
        tasks.add(task(project, "TestTask2", "FAILED", "2020-01-02", "2020-01-02"));

        tasks.add(task(project, "TestTask3", "SUCCEEDED", "2020-01-03", "2020-01-03"));

        when(taskRepository.findTasksInRage(any(), any(), any())).thenReturn(tasks);
        LinkedHashMap<String, Integer> topFailedTasksStats = taskTrackerService.findTopFailedTasksStats(new Project(), new Date(), new Date(), 10);

        assertNotNull(topFailedTasksStats);
        assertThat(topFailedTasksStats.size(), equalTo(2));
        assertThat(topFailedTasksStats, hasEntry("TestTask2", 4));
        assertThat(topFailedTasksStats, hasEntry("TestTask1", 2));

//        Check order of map keys
        List<String> keys = new ArrayList<>(topFailedTasksStats.keySet());
        assertThat(keys.get(0), equalTo("TestTask2"));
        assertThat(keys.get(1), equalTo("TestTask1"));
    }


    @Test
    public void getTopLongTasksStats() {
        List<Task> tasks = new ArrayList<>();
//        There are 2 SUCCEEDED TestTask1 tasks with execution time: 4h, 1h => median = (1 + 4) * 0.5 * 60 * 60 = 9 000s
        tasks.add(task(project, "TestTask1", "SUCCEEDED",
                "2020-01-01T08:00:00.000+0000", "2020-01-01T08:00:00.000+0000", "2020-01-01T12:00:00.000+0000"));
        tasks.add(task(project, "TestTask1", "SUCCEEDED",
                "2020-01-02T08:00:00.000+0000", "2020-01-02T08:00:00.000+0000", "2020-01-02T09:00:00.000+0000"));
        tasks.add(task(project, "TestTask1", "FAILED",
                "2020-01-03T08:00:00.000+0000", "2020-01-03T08:00:00.000+0000", "2020-01-03T19:00:00.000+0000"));

//        There are 3 SUCCEEDED TestTask2 tasks with execution time: 20m, 30m, 60m => median = 30 * 60 = 1800s
        tasks.add(task(project, "TestTask2", "SUCCEEDED",
                "2020-01-15T12:00:00.000+0000", "2020-01-15T12:00:00.000+0000", "2020-01-15T12:20:00.000+0000"));
        tasks.add(task(project, "TestTask2", "SUCCEEDED",
                "2020-01-16T12:00:00.000+0000", "2020-01-16T12:00:00.000+0000", "2020-01-16T12:30:00.000+0000"));
        tasks.add(task(project, "TestTask2", "SUCCEEDED",
                "2020-01-17T12:00:00.000+0000", "2020-01-17T12:00:00.000+0000", "2020-01-17T13:00:00.000+0000"));
        tasks.add(task(project, "TestTask2", "RUNNING",
                "2020-01-18T12:00:00.000+0000", "2020-01-18T12:00:00.000+0000"));
        tasks.add(task(project, "TestTask2", "FAILED",
                "2020-01-20T12:00:00.000+0000", "2020-01-20T12:00:00.000+0000", "2020-01-20T17:00:00.000+0000"));

        when(taskRepository.findTasksInRage(any(), any(), any())).thenReturn(tasks);
        LinkedHashMap<String, Integer> topFailedTasksStats = taskTrackerService.findTopLongTasksStats(new Project(), new Date(), new Date(), 10);

        assertNotNull(topFailedTasksStats);
        assertThat(topFailedTasksStats.size(), equalTo(2));
        assertThat(topFailedTasksStats, hasEntry("TestTask2", 1800));
        assertThat(topFailedTasksStats, hasEntry("TestTask1", 9000));

//        Check order of map keys
        List<String> keys = new ArrayList<>(topFailedTasksStats.keySet());
        assertThat(keys.get(0), equalTo("TestTask1"));
        assertThat(keys.get(1), equalTo("TestTask2"));

    }

    @Test
    public void getTasksDistributionWeek() {
        List<Task> tasks = new ArrayList<>();

//        There are 3 tasks started at first week
        tasks.add(task(project, "TestTask1", "SUCCEEDED",
                "2020-01-01T08:00:00.000+0000", "2020-05-05T08:00:00.000+0000", "2020-05-05T12:00:00.000+0000"));
        tasks.add(task(project, "TestTask1", "SUCCEEDED",
                "2020-01-02T08:00:00.000+0000", "2020-05-04T08:00:00.000+0000", "2020-05-04T09:00:00.000+0000"));
        tasks.add(task(project, "TestTask1", "FAILED",
                "2020-01-03T08:00:00.000+0000", "2020-05-02T08:00:00.000+0000", "2020-05-02T19:00:00.000+0000"));

//        There are 2 tasks started at second week
        tasks.add(task(project, "TestTask2", "SUCCEEDED",
                "2020-01-15T12:00:00.000+0000", "2020-05-08T12:00:00.000+0000", "2020-05-08T12:20:00.000+0000"));
        tasks.add(task(project, "TestTask2", "FAILED",
                "2020-01-20T12:00:00.000+0000", "2020-05-09T12:00:00.000+0000", "2020-05-09T17:00:00.000+0000"));

        Date startDate = DateTimePatterns.getDateFromFormattedDateString("2020-05-01");
        Date endDate = DateTimePatterns.getDateFromFormattedDateString("2020-05-10");

        when(taskRepository.findTasksInRage(any(), any(), any())).thenReturn(tasks);
        Map<Date, Integer> tasksDistribution = taskTrackerService.getTasksDistribution(new Project(), startDate, endDate, Frequency.WEEK);

        Date d1 = DateTimePatterns.getDateFromFormattedDateString("2020-05-01");
        Date d2 = DateTimePatterns.getDateFromFormattedDateString("2020-05-08");

        assertNotNull(tasksDistribution);
        assertThat(tasksDistribution.size(), equalTo(2));
        assertThat(tasksDistribution, hasEntry(d1, 3));
        assertThat(tasksDistribution, hasEntry(d2, 2));

        //        Check order of map keys
        List<Date> keys = new ArrayList<>(tasksDistribution.keySet());
        assertThat(keys.get(0), equalTo(d1));
        assertThat(keys.get(1), equalTo(d2));

    }

    @Test
    public void getTasksDistributionHour() {
        List<Task> tasks = new ArrayList<>();

//        There is 1 task
        tasks.add(task(project, "TestTask1", "SUCCEEDED",
                "2020-01-01T08:00:00.000+0000", "2020-05-01T08:00:00.000+0000", "2020-05-05T12:00:00.000+0000"));

//        There is 1 task started after midnight
        tasks.add(task(project, "TestTask2", "FAILED",
                "2020-01-20T12:00:00.000+0000", "2020-05-02T00:30:00.000+0000", "2020-05-09T17:00:00.000+0000"));

        Date startDate = DateTimePatterns.getDateFromFormattedDateString("2020-05-01");
        Date endDate = DateTimePatterns.getDateFromFormattedDateString("2020-05-02");

        when(taskRepository.findTasksInRageTruncHour(any(), any(), any())).thenReturn(tasks);
        Map<Date, Integer> tasksDistribution = taskTrackerService.getTasksDistribution(new Project(), startDate, endDate, Frequency.HOUR);

        Date d1 = DateTimePatterns.getDateFromFormattedDateString("2020-05-01T08:00:00.000+0000");
        Date d2 = DateTimePatterns.getDateFromFormattedDateString("2020-05-02T00:00:00.000+0000");

        assertNotNull(tasksDistribution);
        assertThat(tasksDistribution.size(), equalTo(25));
        assertThat(tasksDistribution, hasEntry(d1, 1));
        assertThat(tasksDistribution, hasEntry(d2, 1));

        //        Check order of map keys
        List<Date> keys = new ArrayList<>(tasksDistribution.keySet());
        assertThat(keys.get(8), equalTo(d1));
        assertThat(keys.get(24), equalTo(d2));
    }

    @Test
    public void findTopErrors() {
        Task task1 = task(project, "TestTask3", "FAILED", "2020-01-01", "2020-01-05");
        List<TaskError> errors1 = new ArrayList<>();
        errors1.add(error(task1, "Dataset main is empty", TaskError.Type.NO_DATA_ERROR));

        Task task2 = task(project, "TestTask1", "FAILED", "2020-01-01", "2020-01-05");
        List<TaskError> errors2 = new ArrayList<>();
        errors2.add(error(task2, "Dataset TestTask1 is empty", TaskError.Type.DATA_LOAD_ERROR));
        errors2.add(error(task2, "Dataset TestTask1 is empty", TaskError.Type.DATA_LOAD_ERROR));
        errors2.add(error(task2, "Dataset TestTask1 is empty", TaskError.Type.DATA_LOAD_ERROR));

        List<Task> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);

        when(taskRepository.findTasksInRage(any(), any(), any())).thenReturn(tasks);
        when(taskErrorRepository.findByTask(any())).thenReturn(errors1, errors2);

        Date startDate = DateTimePatterns.getDateFromFormattedDateString("2020-05-01");
        Date endDate = DateTimePatterns.getDateFromFormattedDateString("2020-05-02");
        List<TopChart.TopElement> topErrors = taskTrackerService.findTopErrors(project, startDate, endDate, 5);

        assertNotNull(topErrors);
        assertThat(topErrors.size(), equalTo(2));

        assertThat(topErrors.get(0), equalTo(new TopChart.TopElement("DATA_LOAD_ERROR", 3)));
        assertThat(topErrors.get(1), equalTo(new TopChart.TopElement("NO_DATA_ERROR", 1)));
    }

    @Test
    public void findTopWarnings() {
        Task task1 = task(project, "TestTask5", "SUCCEEDED", "2020-01-01", "2020-01-05");
        List<Warning> warnings1 = new ArrayList<>();
        warnings1.add(warning(task1, "TestTask5.cnt is NULL"));

        Task task2 = task(project, "TestTask1", "FAILED", "2020-01-01", "2020-01-05");
        List<Warning> warnings2 = new ArrayList<>();
        warnings2.add(warning(task2, "stg.cnt is NULL"));
        warnings2.add(warning(task2, "stg.cnt is NULL"));

        List<Task> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(task2);

        when(taskRepository.findTasksInRage(any(), any(), any())).thenReturn(tasks);
        when(warningRepository.findByTask(any())).thenReturn(warnings1, warnings2);

        Date startDate = DateTimePatterns.getDateFromFormattedDateString("2020-05-01");
        Date endDate = DateTimePatterns.getDateFromFormattedDateString("2020-05-02");
        List<TopChart.TopWarningElement> topWarnings = taskTrackerService.findTopWarnings(project, startDate, endDate, 5);

        assertNotNull(topWarnings);
        assertThat(topWarnings.size(), equalTo(2));

        assertThat(topWarnings.get(0), equalTo(new TopChart.TopWarningElement("stg.cnt is NULL", 2, "TestTask1")));
        assertThat(topWarnings.get(1), equalTo(new TopChart.TopWarningElement("TestTask5.cnt is NULL", 1, "TestTask5")));
    }

    @Test
    public void tableToColumn() {
        BiFunction<String, String, TaskStats> createTaskStats = (taskName, columnName) -> {
            Task task = new Task();
            task.setName(taskName);

            TaskStats taskStats = new TaskStats();
            taskStats.setColumn(columnName);
            taskStats.setId(1L);
            taskStats.setTask(task);

            return taskStats;
        };

        TaskTrackerService taskTrackerService = mock(TaskTrackerService.class);

        List<TaskStats> taskStats = Lists.newArrayList();
        taskStats.add(createTaskStats.apply("TestTask1", "stg.stg_task_1.row_cnt"));
        taskStats.add(createTaskStats.apply("TestTask1", "stg.stg_task_1.row_cnt"));
        taskStats.add(createTaskStats.apply("TestTask2", "stg.stg_task_2.row_cnt"));
        taskStats.add(createTaskStats.apply("TestTask2", "stg.stg_task_2.row_cnt"));
        taskStats.add(createTaskStats.apply("TestTask2", "stg.stg_task_2.row_cnt"));
        taskStats.add(createTaskStats.apply("TestTask3", "stg.stg_task_3.row_cnt"));

        when(taskTrackerService.findDistinctTaskStatsByProject(any(), any())).thenReturn(taskStats);
        when(taskTrackerService.tableToColumn(any(), any())).thenCallRealMethod();

        List<TableModel> tableModels = taskTrackerService.tableToColumn(null, null);

        assertThat(tableModels.size(), equalTo(3));
    }

    private void assertTaskDatesSizeEquals(final TasksReportModel tasksReportModel,
                                           final Map<String, Integer> dateToSize) {
        dateToSize.forEach((scoringDate, size) -> {
                    assertThat(tasksReportModel.getDates()
                                    .stream()
                                    .filter(dateTask -> dateTask.getDate().equals(scoringDate))
                                    .findFirst()
                                    .orElse(null)
                                    .getTasks(),
                            hasSize(size));
                }
        );
    }

}